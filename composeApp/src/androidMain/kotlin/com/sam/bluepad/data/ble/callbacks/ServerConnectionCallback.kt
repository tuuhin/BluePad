package com.sam.bluepad.data.ble.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.BLESyncACKFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncData
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.toKotlinUuid

private const val TAG = "SERVER_CALLBACK"
private const val NONCE_SIZE = 8

private typealias GATTSendResponse = (device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray?) -> Unit
private typealias GATTNotifyCharacteristicsChanged = (device: BluetoothDevice, characteristics: BluetoothGattCharacteristic, confirm: Boolean, value: ByteArray) -> Boolean

@SuppressLint("MissingPermission")
class ServerConnectionCallback(
    provider: LocalDeviceInfoProvider,
    repository: ExternalDevicesRepository,
    private val protoBuf: ProtoBuf,
    private val encoder: BytesEncoder,
    private val randomGenerator: RandomGenerator,
    private val platformInfoProvider: PlatformInfoProvider,
) : BluetoothGattServerCallback() {

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _localNonceMap = ConcurrentHashMap<String, String>()
    private val _cccDescriptorMap = ConcurrentHashMap<String, Boolean>()

    private val _deviceInfo = provider.readDeviceInfo.stateIn(
        scope = _scope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val _savedDevices = repository.getAllDevices()
        .filterIsInstance<Resource.Success<List<ExternalDeviceModel>, Exception>>()
        .map { it.data }
        .stateIn(_scope, SharingStarted.Eagerly, emptyList())

    private var _sendResponse: GATTSendResponse? = null
    private var _notifyCharacteristicsChanged: GATTNotifyCharacteristicsChanged? = null
    private var _onServiceAdded: (() -> Unit)? = null

    fun setOnSendResponse(callback: GATTSendResponse) {
        _sendResponse = callback
    }

    fun setNotifyCharacteristicsChanged(callback: GATTNotifyCharacteristicsChanged) {
        _notifyCharacteristicsChanged = callback
    }

    fun setOnServiceAdded(onServiceAdded: () -> Unit = {}) {
        _onServiceAdded = onServiceAdded
    }

    private val _incomingPeerData = MutableStateFlow<List<BLEPeerData>>(emptyList())
    val incomingPeerData: Flow<List<BLEPeerData>>
        get() = _incomingPeerData.asStateFlow()

    private val _incomingSyncRequest = Channel<AdvertiserSyncEvent>(Channel.CONFLATED)
    val syncRequestEvents: Flow<AdvertiserSyncEvent>
        get() = _incomingSyncRequest.consumeAsFlow()

    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        if (device == null || status != BluetoothGatt.GATT_SUCCESS) return

        val bondState = try {
            when (device.bondState) {
                BluetoothDevice.BOND_NONE -> "NO BOND"
                BluetoothDevice.BOND_BONDED -> "BONDED"
                BluetoothDevice.BOND_BONDING -> "BONDING"
                else -> null
            }
        } catch (e: SecurityException) {
            Logger.e(TAG, e) { "MISSING CONNECT PERMISSION" }
            null
        }

        val state = when (newState) {
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
            else -> null
        }
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            _localNonceMap.remove(device.address)
            _cccDescriptorMap.remove(device.address)
        }
        Logger.d(TAG) { "DEVICE IDENTIFIER:${device.address} BOND STATE:$bondState CONNECTION STATE:$state" }
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
        if (device == null) return
        Logger.d(TAG) { "DEVICE IDENTIFIER:${device.address} NEW MAX_TRANSMISSION_UNIT:$mtu" }
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "SOME ERROR IN ADDING THE SERVICE: $status" }
            return
        }
        if (service == null) return
        Logger.i(TAG) { "SERVICE :${service.uuid} ADDED" }
        _onServiceAdded?.invoke()
    }

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (device == null || characteristic == null) {
            sendFailedResponse(device, requestId, offset)
            return
        }
        // handle discovery service here only
        if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.DEVICE_INFO_SERVICE_ID) {
            if (characteristic.uuid.toKotlinUuid() != BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID) {
                Logger.w(TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}" }
                sendFailedResponse(device, requestId, offset)
                return
            }
            Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }

            val deviceInfo = _deviceInfo.value ?: run {
                Logger.w(TAG) { "CANNOT READ DEVICE INFO" }
                sendFailedResponse(device, requestId, offset)
                return
            }

            val value = try {
                val nonce = randomGenerator.generateRandomBytes(size = NONCE_SIZE)
                val peerData = BLEPeerData(
                    deviceId = deviceInfo.deviceId,
                    deviceOs = platformInfoProvider.platformOS,
                    deviceName = deviceInfo.name,
                    nonce = nonce.decodeToString(),
                )
                val bytes = protoBuf.encodeToByteArray<BLEPeerData>(peerData)
                Logger.d(TAG) { "RESPONDING WITH DATA SIZE:${bytes.size}" }
                bytes
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                sendFailedResponse(device, requestId, offset)
                return
            }
            Logger.d(TAG) { "SENDING SUCCESS READ RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
            _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            return
        }

        // handle the sync service here
        if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.SYNC_SERVICE_ID) {
            if (characteristic.uuid.toKotlinUuid() != BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID) {
                Logger.w(TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}" }
                sendFailedResponse(device, requestId, offset)
                return
            }

            Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }
            val deviceInfo = _deviceInfo.value ?: run {
                Logger.e(TAG) { "CANNOT READ DEVICE INFO" }
                sendFailedResponse(device, requestId, offset)
                return
            }

            val value = try {
                val nonce = randomGenerator.generateRandomBytes(NONCE_SIZE).let { nonceBytes ->
                    encoder.encodeBytes(nonceBytes).apply { _localNonceMap[device.address] = this }
                }
                val data = BLESyncData.BLEAdvertiseData(deviceInfo.deviceId, nonce, true)
                val bytes = protoBuf.encodeToByteArray<BLESyncData.BLEAdvertiseData>(data)
                Logger.d(TAG) { "RESPONDING WITH DATA SIZE:${bytes.size}" }
                bytes
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                sendFailedResponse(device, requestId, offset)
                return
            }

            Logger.d(TAG) { "SENDING SUCCESS READ RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
            _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            return
        }
        // invalids
        Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
        sendFailedResponse(device, requestId, offset)
    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        // ensure we have some data
        if (device == null || characteristic == null || value == null) {
            sendFailedResponse(device, requestId, offset, responseNeeded)
            return
        }

        if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.DEVICE_INFO_SERVICE_ID) {
            if (characteristic.uuid.toKotlinUuid() != BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID) {
                Logger.w(TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}" }
                sendFailedResponse(device, requestId, offset)
                return
            }
            Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }

            try {
                val peerData = protoBuf.decodeFromByteArray<BLEPeerData>(value)
                _incomingPeerData.update { devices -> (devices + peerData).distinctBy { it.deviceId } }
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                sendFailedResponse(device, requestId, offset, responseNeeded)
                return
            }

            if (!responseNeeded) return
            Logger.d(TAG) { "SENDING SUCCESS WRITE RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
            _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            return
        }
        // handle the sync service here
        if (characteristic.service.uuid.toKotlinUuid() == BLEConstants.SYNC_SERVICE_ID) {
            if (characteristic.uuid.toKotlinUuid() != BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID) {
                Logger.w(TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}" }
                sendFailedResponse(device, requestId, offset)
                return
            }
            Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }

            try {
                val response = protoBuf.decodeFromByteArray<BLESyncData.BLEAdvertiseResponse>(value)
                Logger.d(TAG) { "WRITE REQUESTED ACCEPTED CHARACTERISTIC ID: ${characteristic.uuid}" }

                val savedNonce = _localNonceMap[device.address]
                val externalDevice = _savedDevices.value.find { it.id == response.senderID }

                val data = when {
                    savedNonce == null -> BLESyncData.BLESyncACKFailed(reason = BLESyncACKFailedReason.INVALID_INCOMING_DATA)
                    savedNonce != response.nonce ->
                        BLESyncData.BLESyncACKFailed(BLESyncACKFailedReason.TAMPERED_DATA)

                    response.receiverID != _deviceInfo.value?.deviceId ->
                        BLESyncData.BLESyncACKFailed(reason = BLESyncACKFailedReason.INVALID_RECEIVER)

                    externalDevice == null -> BLESyncData.BLESyncACKFailed(BLESyncACKFailedReason.UNKNOWN_SENDER)
                    else -> BLESyncData.BLESyncACKSuccess(
                        nonce = response.nonce,
                        deviceAddress = device.address
                    )
                }

                val bytes = protoBuf.encodeToByteArray<BLESyncData>(data)
                _notifyCharacteristicsChanged?.invoke(
                    device,
                    characteristic,
                    false,
                    bytes
                )

                if (externalDevice == null) {
                    Logger.d(TAG) { "RECEIVER DEVICE IS NOT KNOWN" }
                    sendFailedResponse(device, requestId, offset, responseNeeded)
                    return
                }
                val event = AdvertiserSyncEvent.ForeignSyncRequest(externalDevice)
                _incomingSyncRequest.trySend(event)
                when (data) {
                    is BLESyncData.BLESyncACKSuccess -> Logger.d(TAG) { "NOTIFICATION ON CHARACTERISTIC : ${characteristic.uuid} SEND SUCCESS" }
                    is BLESyncData.BLESyncACKFailed -> Logger.d(TAG) { "NOTIFICATION ON CHARACTERISTIC : ${characteristic.uuid} FAILED REASON :${data.reason}" }
                    else -> {}
                }
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                sendFailedResponse(device, requestId, offset)
                return
            }
            if (!responseNeeded) return
            Logger.d(TAG) { "SENDING WRITE RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}" }
            _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            return
        }
        // invalids
        Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
        sendFailedResponse(device, requestId, offset, responseNeeded)
    }

    override fun onDescriptorReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        descriptor: BluetoothGattDescriptor?
    ) {
        if (device == null || descriptor == null) {
            sendFailedResponse(device, requestId, offset, false)
            return
        }
        Logger.i(TAG) { "READ REQUEST DESCRIPTOR ID ${descriptor.uuid} CHARACTERISTIC ID : ${descriptor.characteristic.uuid}" }

        if (descriptor.characteristic.uuid.toKotlinUuid() != BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID) {
            Logger.d(TAG) { "INVALID CHARACTERISTICS PROVIDED :${descriptor.characteristic.uuid}" }
            sendFailedResponse(device, requestId, offset, false)
            return
        }

        if (descriptor.uuid.toKotlinUuid() != BLEConstants.CCC_DESCRIPTOR) {
            Logger.d(TAG) { "INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED" }
            sendFailedResponse(device, requestId, offset, false)
            return
        }

        val isEnabled = _cccDescriptorMap[device.address] ?: false
        val isIndication =
            descriptor.characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        val bytes = when (isEnabled) {
            true if (isIndication) -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            true -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            false -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        val bytesAsString = bytes.joinToString("-") { it.toHexString() }
        Logger.d(TAG) { "DESCRIPTOR READ VALUE : $bytesAsString" }
        _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, bytes)
    }

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        descriptor: BluetoothGattDescriptor?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        if (device == null || descriptor == null || value == null) {
            sendFailedResponse(device, requestId, offset, responseNeeded)
            return
        }
        Logger.i(TAG) { "WRITE REQUEST DESCRIPTOR ID ${descriptor.uuid} CHARACTERISTIC ID : ${descriptor.characteristic.uuid}" }

        if (descriptor.characteristic.uuid.toKotlinUuid() != BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID) {
            sendFailedResponse(device, requestId, offset, false)
            return
        }

        if (descriptor.uuid.toKotlinUuid() != BLEConstants.CCC_DESCRIPTOR) {
            Logger.d(TAG) { "INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED" }
            sendFailedResponse(device, requestId, offset, responseNeeded)
            return
        }
        val isNotifyEnabled = when {
            value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) -> true
            value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) -> true
            value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) -> false
            else -> {
                sendFailedResponse(device, requestId, offset, responseNeeded)
                return
            }
        }

        if (descriptor.characteristic.uuid.toKotlinUuid() != BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID) {
            Logger.d(TAG) { "INVALID CHARACTERISTICS PROVIDED :${descriptor.characteristic.uuid}" }
            sendFailedResponse(device, requestId, offset, responseNeeded)
            return
        }

        _cccDescriptorMap[device.address] = isNotifyEnabled
        val bytesAsString = value.joinToString("-") { it.toHexString() }
        Logger.d(TAG) { "UPDATED DESCRIPTOR VALUE :$bytesAsString" }

        if (!responseNeeded) return
        _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
    }

    private fun sendFailedResponse(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        responseNeeded: Boolean = true
    ) {
        if (!responseNeeded) return
        _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
    }

    fun cleanUp() {
        // clears everything on done
        if (_scope.isActive) _scope.cancel()
        // clear the maps
        _localNonceMap.clear()
        _cccDescriptorMap.clear()
        // clear the maps
        _incomingPeerData.value = emptyList()
    }
}