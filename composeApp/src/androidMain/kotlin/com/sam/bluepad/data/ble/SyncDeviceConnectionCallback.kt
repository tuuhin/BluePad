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
import com.sam.bluepad.domain.ble.models.BLESyncData
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.toKotlinUuid

private const val TAG = "SYNC_CONNECTION_CALLBACK"

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class SyncDeviceConnectionCallback(
    provider: LocalDeviceInfoProvider,
    private val protoBuf: ProtoBuf,
) : BluetoothGattCallback() {

    private val _scope = CoroutineScope(Dispatchers.IO)

    private val _eventsChannel = Channel<BLEDeviceSyncEvent>(Channel.CONFLATED)
    private val _errorChannel = Channel<Exception>(Channel.CONFLATED)

    val eventsFlow = _eventsChannel.receiveAsFlow()
    val errorFlow = _errorChannel.receiveAsFlow()

    private val _incomingData = ConcurrentHashMap<String, BLESyncData.BLEAdvertiseData>()

    private val deviceInfo = provider.readDeviceInfo.stateIn(
        scope = _scope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT READ CONNECTION STATE" }
            _errorChannel.trySend(Exception("Connection state status failed"))
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
                _eventsChannel.trySend(BLEDeviceSyncEvent.ConnectionSuccess)
                gatt?.requestMtu(BLEConstants.REQUESTED_MTU)
                Logger.d(TAG) { "REQUESTING HIGHER MTU: ${BLEConstants.REQUESTED_MTU}" }
            }

            BluetoothGatt.STATE_DISCONNECTED -> {
                // device disconnected
                _eventsChannel.trySend(BLEDeviceSyncEvent.DeviceDisconnected)
            }

            else -> {}
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT DISCOVER SERVICES" }
            _errorChannel.trySend(Exception("Device discovery status failed"))
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
        _eventsChannel.trySend(BLEDeviceSyncEvent.ServicesDiscovered)
        gatt.readCharacteristic(characteristic)
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT UPDATE MTU" }
            _errorChannel.trySend(Exception("MTU changed failed"))
            return
        }
        Logger.d(TAG) { "UPDATED MTU :$mtu" }
        Logger.d(TAG) { "REQUESTED SERVICE DISCOVERY" }
        gatt?.discoverServices()
    }

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onCharacteristicRead(gatt, characteristic, characteristic.value, status)
    }


    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT READ CHARACTERISTIC : ${characteristic.uuid}" }
            _errorChannel.trySend(Exception("Cannot read characteristics"))
            return
        }
        if (characteristic.uuid.toKotlinUuid() != BLEConstants.SYNC_CHARACTERISTICS_ID) {
            Logger.w(TAG) { "INVALID CHARACTERISTICS ID" }
            return
        }
        try {
            val advertiseData = protoBuf.decodeFromByteArray<BLESyncData.BLEAdvertiseData>(value)
            _eventsChannel.trySend(
                BLEDeviceSyncEvent.AdvertisingDataRead(
                    characteristicsId = characteristic.uuid.toKotlinUuid(),
                    data = advertiseData
                )
            )
            Logger.d(TAG) { "ADVERTISE DATA RECEIVED DEVICE_ID:${advertiseData.deviceId}" }
            gatt.device?.address?.let { _incomingData.put(it, advertiseData) }

            // TODO: CHECK IF ITS VALID OR NOT
            // turn on notification
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
            _errorChannel.trySend(Exception("Cannot send write to characteristics"))
            return
        }
        if (characteristic?.uuid?.toKotlinUuid() != BLEConstants.SYNC_CHARACTERISTICS_ID) return
        Logger.d(TAG) { "WRITING ADVERTISING RESPONSE CHARACTERISTICS DONE" }
        _eventsChannel.trySend(BLEDeviceSyncEvent.AdvertisingResponseSend)
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray
    ) {
        super.onDescriptorRead(gatt, descriptor, status, value)
        Logger.e(TAG) { "SOME !!!!!!!!!!!!!!!!" }
    }

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
                _incomingData.getOrDefault(it.address, null)
            } ?: return
            // characteristics to write
            val outgoingData = BLESyncData.BLEAdvertiseResponse(
                nonce = advertiseData.nonce,
                receiverDeviceId = advertiseData.deviceId,
                currentDeviceId = currentDeviceInfo.deviceId
            )
            val syncWrite = protoBuf.encodeToByteArray<BLESyncData.BLEAdvertiseResponse>(outgoingData)
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
            val result = protoBuf.decodeFromByteArray<BLESyncData.BLESyncAcknowledgement>(value)
            Logger.i(TAG) { "SYNC ACK DATA FOUND" }
            _eventsChannel.trySend(BLEDeviceSyncEvent.AdvertisingAcknowledgmentReceived(result))

            // data is found so turn this off now
            // TODO: Only the android system stop keeping track of the notification
            val isDisabled = gatt.setCharacteristicNotification(characteristic, false)
            Logger.d(TAG) { "NOTIFICATION LISTENER DISABLED:$isDisabled" }


        } catch (e: SerializationException) {
            Logger.e(TAG, e) { "CANNOT SERIALIZE THE DATA" }
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, e) { "INVALID INPUT" }
        }
    }

    fun cancel() {
        _scope.cancel()
        _incomingData.clear()
    }
}