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
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.sync.SyncInPayloadManager
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.uuid.toKotlinUuid

private const val TAG = "SERVER_CALLBACK"

typealias GATTSendResponse = (device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray?) -> Unit
typealias GATTNotifyCharacteristicsChanged = (device: BluetoothDevice, characteristics: BluetoothGattCharacteristic, confirm: Boolean, value: ByteArray) -> Boolean

@SuppressLint("MissingPermission")
class ServerConnectionCallback(
    provider: LocalDeviceInfoProvider,
    repository: ExternalDevicesRepository,
    private val methodHandler: ServerCallbackMethodHandler,
    private val syncInManager: SyncInPayloadManager,
) : BluetoothGattServerCallback() {

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
    private var _onServiceAdded: (() -> Unit)? = null

    fun setOnSendResponse(callback: GATTSendResponse) {
        _sendResponse = callback
    }

    fun setNotifyCharacteristicsChanged(callback: GATTNotifyCharacteristicsChanged) {
        methodHandler.notifyCharacteristicsChanged = callback
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
            methodHandler.clearDeviceInfo(device.address)
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
        val serviceId = characteristic.service.uuid.toKotlinUuid()
        val characteristicsId = characteristic.uuid.toKotlinUuid()

        val value = when (characteristicsId) {
            // HANDLE DEVICE DISCOVERY ADVERTISEMENT HERE
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceId == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }
                methodHandler.handleDeviceReadRequest(_deviceInfo.value) { reason ->
                    sendFailedResponse(device, requestId, offset, true, reason)
                    return
                }
            }
            // HANDLE SYNC SERVICE PROXIMITY CHECK ADVERTISEMENT HERE
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }
                methodHandler.handleProximityReadRequest(
                    device = device,
                    currentDeviceInfo = _deviceInfo.value
                ) { reason ->
                    sendFailedResponse(device, requestId, offset, true, reason)
                    return
                }
            }

            else -> {
                Logger.w(TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}" }
                sendFailedResponse(device, requestId, offset, true)
                return
            }
        }
        // if all goes good send success
        sendSuccessResponse(device, requestId, offset, true, value, "SENDING RESPONSE SUCCESS")
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
        // SOME DATA IS PRESENT
        if (device == null || characteristic == null || value == null) {
            sendFailedResponse(device, requestId, offset, responseNeeded)
            return
        }

        val characteristicId = characteristic.uuid.toKotlinUuid()
        val serviceId = characteristic.service.uuid.toKotlinUuid()

        val message = "WRITE RESPONSE FOR CHARACTERISTICS :${characteristic.uuid}"
        val invalidCharReason =
            "NO ASSOCIATE CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}"

        when (characteristicId) {
            // HANDLE DEVICE DISCOVERY ADVERTISEMENT HERE
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceId == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }
                methodHandler.handleDeviceWriteRequest(value) { reason ->
                    sendFailedResponse(device, requestId, offset, responseNeeded, reason)
                    return
                }
            }

            // HANDLE SYNC AND PROXIMITY SERVICE FROM HERE
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }
                methodHandler.handleProximityWriteRequest(
                    device = device,
                    characteristic = characteristic,
                    value = value,
                    savedDevices = _savedDevices.value,
                    currentDeviceInfo = _deviceInfo.value
                ) { reason ->
                    sendFailedResponse(device, requestId, offset, responseNeeded, reason)
                    return
                }
            }

            // HANDLE SYNC AND PROXIMITY SERVICE FROM HERE
            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }
                methodHandler.handleDataWriteRequest(
                    device = device,
                    characteristic = characteristic,
                    value = value,
                    onSessionStart = {
                        Logger.d(TAG) { "SYNC SESSION STARTING" }
                        syncInManager.clearBuffer()
                    },
                    onReadPayload = { _, seq, payload ->
                        syncInManager.addPayloadChunk(seq, payload)
                    },
                    onPayloadTypeChange = { type ->
                        if (type == null) return
                        when (type) {
                            BLESyncDataType.REQUESTED_CONTENT_IDS -> {
                                Logger.d(TAG) { "REQUESTING NOT_MATCHING_CONTENTS" }
                                _scope.launch {
                                    syncInManager.processMetadata()
                                }
                            }

                            else -> {}
                        }
                    },
                    onFailed = { reason ->
                        sendFailedResponse(device, requestId, offset, responseNeeded, reason)
                        return
                    }
                )
            }

            else -> {
                sendFailedResponse(device, requestId, offset, responseNeeded, invalidCharReason)
                return
            }
        }
        // if all goes good send success
        sendSuccessResponse(device, requestId, offset, responseNeeded, value, message)
    }

    override fun onDescriptorReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        descriptor: BluetoothGattDescriptor?
    ) {
        if (device == null || descriptor == null) {
            sendFailedResponse(device, requestId, offset, true)
            return
        }
        Logger.i(TAG) { "READ REQUEST DESCRIPTOR ID ${descriptor.uuid} CHARACTERISTIC ID : ${descriptor.characteristic.uuid}" }

        // only handle sync service id
        if (descriptor.characteristic.service.uuid != BLEConstants.SYNC_SERVICE_ID) {
            sendFailedResponse(device, requestId, offset, true, "INVALID SERVICE")
            return
        }

        val bytes = when (descriptor.characteristic.uuid.toKotlinUuid()) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID ->
                methodHandler.handleCCCReadRequest(device, descriptor) { reason ->
                    sendFailedResponse(device, requestId, offset, true, reason)
                }

            else -> {
                sendFailedResponse(device, requestId, offset, true, "Invalid")
                return
            }
        }
        // send success if not failed send success message on done
        sendSuccessResponse(device, requestId, offset, true, bytes, "Descriptor written")
        return

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

        // only handle sync service id
        if (descriptor.characteristic.service.uuid != BLEConstants.SYNC_SERVICE_ID) {
            sendFailedResponse(device, requestId, offset, false, "INVALID SERVICE")
            return
        }

        when (descriptor.characteristic.uuid.toKotlinUuid()) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID ->
                methodHandler.handleCCCWriteRequest(device, descriptor, value) { reason ->
                    sendFailedResponse(device, requestId, offset, responseNeeded, reason)
                }

            else -> {
                sendFailedResponse(device, requestId, offset, responseNeeded, "Invalid")
                return
            }
        }
        // send success if not failed send success message on done
        sendSuccessResponse(device, requestId, offset, responseNeeded, value, "Descriptor written")
        return
    }

    private fun sendFailedResponse(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        responseNeeded: Boolean = true,
        reason: String? = null,
    ) {
        if (!responseNeeded) return
        reason?.let { Logger.w(TAG) { "SENDING GATT FAILURE REASON:$it" } }
        _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
    }

    private fun sendSuccessResponse(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        responseNeeded: Boolean = true,
        value: ByteArray?,
        message: String? = null,
    ) {
        if (!responseNeeded) return
        message?.let { Logger.d(TAG) { "SENDING GATT SUCCESS MESSAGE:$it" } }
        _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, value)
    }


    fun cleanUp() {
        // clears everything on done
        if (_scope.isActive) _scope.cancel()
        // clear the maps
        methodHandler.cleanUp()
        // clear the maps
        _incomingPeerData.value = emptyList()
    }
}