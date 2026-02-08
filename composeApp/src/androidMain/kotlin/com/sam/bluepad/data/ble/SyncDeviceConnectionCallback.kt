package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEDeviceSyncEvent
import com.sam.bluepad.domain.ble.models.BLESyncACKFailedReason
import com.sam.bluepad.domain.ble.models.BLESyncData
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.toKotlinUuid

private const val TAG = "SYNC_CONNECTION_CALLBACK"

@SuppressLint("MissingPermission")
class SyncDeviceConnectionCallback(
    deviceInfoProvider: LocalDeviceInfoProvider,
    externalDevicesRepository: ExternalDevicesRepository,
    private val protoBuf: ProtoBuf,
) : BluetoothGattCallback() {

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var _onEvents: ((BLEDeviceSyncEvent) -> Unit)? = null
    private var _onError: ((Exception) -> Unit)? = null

    fun onEvents(callback: (BLEDeviceSyncEvent) -> Unit) {
        _onEvents = callback
    }

    fun onError(callback: (Exception) -> Unit) {
        _onError = callback
    }

    private val _advertiseDataCache = ConcurrentHashMap<String, BLESyncData.BLEAdvertiseData>()

    private val deviceInfo = deviceInfoProvider.readDeviceInfo.stateIn(
        scope = _scope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val _savedDevices = externalDevicesRepository.getAllDevices()
        .onEach { res ->
            if (res !is Resource.Error) return@onEach
            Logger.e(TAG, res.error) { "SOME ERROR OCCURRED WHILE READING DEVICES" }
        }
        .filterIsInstance<Resource.Success<List<ExternalDeviceModel>, Exception>>()
        .map { res -> res.data }
        .stateIn(_scope, SharingStarted.Eagerly, emptyList())


    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT READ CONNECTION STATE" }
            _onError?.invoke(Exception("Connection state status failed"))
            return
        }
        val domainState = when (newState) {
            BluetoothGatt.STATE_CONNECTED -> BLEConnectionState.CONNECTED
            BluetoothGatt.STATE_DISCONNECTED -> BLEConnectionState.DISCONNECTED
            else -> return
        }
        val deviceAddress = gatt?.device?.address
        Logger.i(TAG) { "DEVICE:$deviceAddress CONNECTION STATE :$domainState" }

        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                // device connected
                _onEvents?.invoke(BLEDeviceSyncEvent.ConnectionSuccess)
                gatt?.requestMtu(BLEConstants.REQUESTED_MTU)
                Logger.d(TAG) { "REQUESTING HIGHER MTU: ${BLEConstants.REQUESTED_MTU}" }
            }

            BluetoothGatt.STATE_DISCONNECTED -> {
                // device disconnected
                _onEvents?.invoke(BLEDeviceSyncEvent.DeviceDisconnected)
            }

            else -> {}
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT DISCOVER SERVICES" }
            _onError?.invoke(Exception("Device discovery status failed"))
            return
        }
        val syncService = gatt?.services
            ?.find { it.uuid.toKotlinUuid() == BLEConstants.SYNC_SERVICE_ID } ?: run {
            Logger.w(TAG) { "SYNC SERVICE NOT FOUND" }
            return
        }
        val characteristic = syncService.characteristics
            .find { it.uuid.toKotlinUuid() == BLEConstants.SYNC_CHARACTERISTICS_ID }
            ?: run {
                Logger.w(TAG) { "SYNC CHARACTERISTICS FOUND" }
                return
            }
        // services discovered
        _onEvents?.invoke(BLEDeviceSyncEvent.ServicesDiscovered)
        gatt.readCharacteristic(characteristic)
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT UPDATE MTU" }
            _onError?.invoke(Exception("MTU changed failed"))
            return
        }
        Logger.d(TAG) { "UPDATED MTU :$mtu" }
        if (!_scope.isActive) Logger.w(TAG) { "REQUESTED SCOPE CANCELLED" }
        _scope.launch {
            Logger.d(TAG) { "REQUESTED SERVICE DISCOVERY" }
            delay(200)
            gatt?.discoverServices()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onCharacteristicRead(gatt, characteristic, characteristic.value, status)
    }

    @Suppress("DEPRECATION")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT READ CHARACTERISTIC : ${characteristic.uuid}" }
            _onError?.invoke(Exception("Cannot read characteristics"))
            return
        }
        if (characteristic.uuid.toKotlinUuid() != BLEConstants.SYNC_CHARACTERISTICS_ID) {
            Logger.w(TAG) { "INVALID CHARACTERISTICS ID" }
            return
        }
        try {
            val syncData = protoBuf.decodeFromByteArray<BLESyncData.BLEAdvertiseData>(value)

            if (!syncData.allowSync) {
                Logger.e(TAG) { "SYNC FLAG MISSING" }
                _onError?.invoke(SyncFlagMissingException())
                return
            }
            val externalDevice = _savedDevices.value.find { it.id == syncData.deviceId } ?: run {
                Logger.w(TAG) { "CANNOT FIND THE GIVEN DEVICE " }
                _onError?.invoke(InvalidReceiverIdException())
                return
            }
            _onEvents?.invoke(
                BLEDeviceSyncEvent.AdvertisingDataRead(
                    characteristicsId = characteristic.uuid.toKotlinUuid(),
                    device = externalDevice
                )
            )

            Logger.d(TAG) { "ADVERTISE DATA RECEIVED DEVICE_ID:${syncData.deviceId}" }
            val deviceAddress = gatt.device?.address ?: return
            _advertiseDataCache[deviceAddress] = syncData

            val isNotificationStarted = gatt.setCharacteristicNotification(characteristic, true)
            Logger.d(TAG) { "NOTIFICATION LISTENER ENABLED:$isNotificationStarted" }

            // write enable to descriptor
            val gattValue = when {
                characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                else -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            val cccDescriptor = characteristic.descriptors
                .find { it.uuid.toKotlinUuid() == BLEConstants.CCC_DESCRIPTOR }
                ?: return

            val isOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccDescriptor, gattValue) == BluetoothStatusCodes.SUCCESS
            } else {
                cccDescriptor.value = gattValue
                gatt.writeDescriptor(cccDescriptor)
            }
            Logger.d(TAG) { "WRITING CCC DESCRIPTOR VALUE ENABLE STATUS:$isOk" }
        } catch (e: SerializationException) {
            Logger.e(TAG, e) { "CANNOT SERIALIZE THE DATA" }
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, e) { "INVALID INPUT" }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "WRITE RESPONSE FAILED" }
            _onError?.invoke(Exception("Cannot send write to characteristics"))
            return
        }
        if (characteristic?.uuid?.toKotlinUuid() != BLEConstants.SYNC_CHARACTERISTICS_ID) return
        Logger.d(TAG) { "WRITING ADVERTISING RESPONSE CHARACTERISTICS DONE" }
        _onEvents?.invoke(BLEDeviceSyncEvent.AdvertisingResponseSend)
    }


    @Suppress("DEPRECATION")
    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "DESCRIPTOR WRITE FAILED" }
            return
        }

        if (descriptor?.characteristic?.uuid?.toKotlinUuid() == BLEConstants.SYNC_CHARACTERISTICS_ID) {
            if (descriptor.uuid.toKotlinUuid() != BLEConstants.CCC_DESCRIPTOR) return
            Logger.d(TAG) { "WRITE CCC DESCRIPTOR ENABLE SUCCEED" }

            // now write the characteristics
            val currentDeviceInfo = deviceInfo.value ?: return
            val advertiseData = gatt?.device?.let {
                _advertiseDataCache.getOrDefault(it.address, null)
            } ?: return
            // characteristics to write
            val outgoingData = BLESyncData.BLEAdvertiseResponse(
                nonce = advertiseData.nonce,
                receiverID = advertiseData.deviceId,
                senderID = currentDeviceInfo.deviceId
            )
            val syncWrite =
                protoBuf.encodeToByteArray<BLESyncData.BLEAdvertiseResponse>(outgoingData)
            val response = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    descriptor.characteristic,
                    syncWrite,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.characteristic.value = syncWrite
                gatt.writeCharacteristic(descriptor.characteristic)
            }
            Logger.d(TAG) { "WRITING ADVERTISING RESPONSE CHARACTERISTICS IS_SUCCESS:$response" }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (gatt == null || characteristic == null) return
        // only use this under API 32
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onCharacteristicChanged(gatt, characteristic, characteristic.value)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        if (characteristic.uuid.toKotlinUuid() != BLEConstants.SYNC_CHARACTERISTICS_ID) return
        try {
            val result = protoBuf.decodeFromByteArray<BLESyncData>(value)
            Logger.i(TAG) { "SYNC ACK DATA FOUND" }
            when (result) {
                is BLESyncData.BLESyncACKFailed -> {
                    val error = InvalidAcknowledgementException(result.reason)
                    Logger.d(TAG, error) { "FAILED ACKNOWLEDGEMENT FOUND REASON:${result.reason}" }
                    _onError?.invoke(error)
                }

                is BLESyncData.BLESyncACKSuccess -> {
                    Logger.i(TAG) { "ACK FOUND" }
                    val ack = BLEDeviceSyncEvent.AdvertisingAcknowledgmentReceived(result)
                    _onEvents?.invoke(ack)
                }

                else -> {}
            }

            // data is found so turn this off now
            // TODO: Only the android system stop keeping track of the notification
            // TODO: Write disable in the gatt descriptor
            val isDisabled = gatt.setCharacteristicNotification(characteristic, false)
            Logger.d(TAG) { "NOTIFICATION LISTENER DISABLED:$isDisabled" }


        } catch (e: SerializationException) {
            Logger.e(TAG, e) { "CANNOT SERIALIZE THE DATA" }
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, e) { "INVALID INPUT" }
        }
    }

    fun onClearCallbacks() {
        Logger.d(TAG) { "CALLBACKS REMOVED" }
        _onError = null
        _onEvents = null
        _advertiseDataCache.clear()
    }

    fun onClose() {
        if (_scope.isActive) {
            Logger.d(TAG) { "CANCELLING SCOPE" }
            _scope.cancel()
        }
        onClearCallbacks()
    }

    private class InvalidReceiverIdException : Exception("Invalid receiver id provided")
    private class InvalidAcknowledgementException(reason: BLESyncACKFailedReason) :
        Exception("Invalid Acknowledgement :${reason.name}")

    private class SyncFlagMissingException : Exception("No sync flag found in the read response")

}