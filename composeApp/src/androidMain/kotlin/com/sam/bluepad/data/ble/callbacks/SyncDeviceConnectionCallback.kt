package com.sam.bluepad.data.ble.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.delegate.BLEConnectorSyncHandlerDelegate
import com.sam.bluepad.data.ble.delegate.PeerProximityConnectorDelegate
import com.sam.bluepad.data.ble.exceptions.BLEConnectionException
import com.sam.bluepad.data.ble.exceptions.GattInvalidStatusException
import com.sam.bluepad.data.ble.utils.toggleNotification
import com.sam.bluepad.data.ble.utils.writeToCharacteristics
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.exceptions.InvalidServiceOrCharacteristicsException
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

private const val TAG = "SYNC_CONNECTION_CALLBACK"

@SuppressLint("MissingPermission")
class SyncDeviceConnectionCallback(
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val dispatcher: PlatformDispatcherProvider,
    private val repository: ExternalDevicesRepository,
    private val syncHandlerDelegate: BLEConnectorSyncHandlerDelegate,
    private val proximityDelegate: PeerProximityConnectorDelegate,
) : BluetoothGattCallback(), LifecycleEventObserver {

    private val _lock = Mutex()
    private val _receiverInfo = HashMap<String, ExternalDeviceModel>()

    private var _onEvents: ((ConnectorSyncEvent) -> Unit)? = null
    private var _onError: ((Throwable) -> Unit)? = null

    fun onEvents(callback: (ConnectorSyncEvent) -> Unit) {
        _onEvents = callback
    }

    fun onError(callback: (Throwable) -> Unit) {
        _onError = callback
    }

    private var _scope: CoroutineScope? = null

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Logger.i(tag = TAG) { "CONNECTOR LIFECYCLE :${event.name}" }
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                _scope?.cancel()
                val scope = CoroutineScope(dispatcher.io.limitedParallelism(10) + SupervisorJob())
                _scope = scope
            }

            Lifecycle.Event.ON_DESTROY -> {
                onClose()
                Logger.i(tag = TAG) { "REMOVING CONNECTOR OBSERVER" }
                source.lifecycle.removeObserver(this)
            }

            else -> {}
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "CANNOT READ CONNECTION STATE" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }
        val deviceAddress = gatt?.device?.address
        when (newState) {
            // device connected
            BluetoothGatt.STATE_CONNECTED -> {
                _onEvents?.invoke(ConnectorSyncEvent.ConnectionSuccess)
                Logger.i(tag = TAG) { "DEVICE:$deviceAddress CONNECTED" }

                val isMtuRequested = gatt?.requestMtu(BLEConstants.REQUESTED_MTU) ?: false
                Logger.d(tag = TAG) { "REQUESTING HIGHER MTU: ${BLEConstants.REQUESTED_MTU} STATUS:$isMtuRequested" }
            }

            // device disconnected
            BluetoothGatt.STATE_DISCONNECTED -> {
                Logger.i(tag = TAG) { "DEVICE:$deviceAddress DIS_CONNECTED" }
                _onEvents?.invoke(ConnectorSyncEvent.DeviceDisconnected)
            }

            else -> {}
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (gatt == null) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "CANNOT DISCOVER SERVICES" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }
        Logger.d(tag = TAG) { "SERVICES DISCOVERED" }
        launchIfActive {
            val servicesIsEmpty = gatt.services.isEmpty()
            if (servicesIsEmpty) {
                // a bit of input delay to load the service into the buffer
                delay(100.milliseconds)
            }
            Logger.d(tag = TAG) { "REQUESTING HANDSHAKE FOR SYNC" }
            val response = requestHandshakeCharacteristics(gatt = gatt)
            response.getOrElse { err -> _onError?.invoke(err) }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "CANNOT UPDATE MTU" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }
        Logger.d(tag = TAG) { "GATT CONNECTION MTU UPDATED TO :$mtu" }
        // requesting service discovery after mtu updated
        val isDiscoveryRequested = gatt?.discoverServices() ?: false
        Logger.d(tag = TAG) { "REQUESTED SERVICE DISCOVERY STARTED:$isDiscoveryRequested" }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onCharacteristicRead(gatt, characteristic, characteristic.value ?: byteArrayOf(), status)
    }


    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "CANNOT READ CHARACTERISTIC : ${characteristic.uuid} STATUS: $status" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }

        val characteristicsId = characteristic.uuid.toKotlinUuid()
        val serviceId = characteristic.service.uuid.toKotlinUuid()

        when (characteristicsId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if serviceId == BLEConstants.SYNC_SERVICE_ID -> launchIfActive {

                val deviceInfo = deviceInfoProvider.readDeviceInfo.first()

                val result = proximityDelegate.handleHandshakeRead(
                    deviceAddress = gatt.device.address,
                    value = value,
                    deviceInfo = deviceInfo,
                    onReadSuccess = { gatt.toggleNotification(characteristic, true) },
                    savedDevices = { id -> repository.getDeviceByUuid(id) },
                )
                result.fold(
                    onSuccess = { device ->
                        val deviceAddress = gatt.device.address
                        _lock.withLock { _receiverInfo[deviceAddress] = device }
                    },
                    onFailure = { err -> _onEvents?.invoke(ConnectorSyncEvent.HandshakeFailed(err.message)) },
                )
            }

            else -> {
                Logger.w(tag = TAG) { "NO READ METHOD PRESENT FOR CHARACTERISTICS:$characteristicsId SERVICE:$serviceId" }
                _onError?.invoke(BLEConnectionException.InvalidCharacteristicsHandlerException())
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int,
    ) {
        if (gatt == null || characteristic == null) return

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "WRITE RESPONSE FAILED" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }

        val characteristicsId = characteristic.uuid.toKotlinUuid()
        val serviceId = characteristic.service.uuid.toKotlinUuid()

        when (characteristicsId) {
            // proximity characteristics write handled no extra handler for it
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if serviceId == BLEConstants.SYNC_SERVICE_ID -> return
            // it's a provided characteristics but adding no handler
            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if serviceId == BLEConstants.SYNC_SERVICE_ID -> return

            else -> {
                Logger.w(tag = TAG) { "NO WRITE RESPONSE EXCEPTED FROM CHARACTERISTICS:$characteristicsId SERVICE:$serviceId" }
                _onError?.invoke(BLEConnectionException.InvalidCharacteristicsHandlerException())
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int,
    ) {
        if (gatt == null || descriptor == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onDescriptorRead(gatt, descriptor, status, descriptor.value ?: byteArrayOf())
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray,
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "DESCRIPTOR READ FAILED STATUS CODE:$status" }
            return
        }
        val descriptorId = descriptor.uuid.toKotlinUuid()
        val serviceId = descriptor.characteristic.service.uuid.toKotlinUuid()
        val characteristicId = descriptor.characteristic.uuid.toKotlinUuid()

        when (descriptorId) {
            BLEConstants.CCC_DESCRIPTOR -> launchIfActive {
                proximityDelegate.onEnabledDisabledCCCDescriptor(
                    address = gatt.device.address,
                    characteristicId = characteristicId,
                    bytes = value,
                    onWriteBytes = { bytes -> gatt.writeToCharacteristics(descriptor.characteristic, bytes) },
                    onToggleNotification = { uuid, enable ->
                        val notificationCharacteristic = gatt.getService(serviceId.toJavaUuid())
                            .getCharacteristic(uuid.toJavaUuid()) ?: return@onEnabledDisabledCCCDescriptor false
                        gatt.toggleNotification(notificationCharacteristic, enable)
                    },
                )
            }

            else -> {
                Logger.d(tag = TAG) { "DESCRIPTOR WRITE PERFORMED ON A NON CCC DESCRIPTOR" }
                _onError?.invoke(BLEConnectionException.InvalidCharacteristicsHandlerException())
            }
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int,
    ) {
        if (gatt == null || descriptor == null) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "DESCRIPTOR WRITE FAILED STATUS: $status" }
            return
        }

        val descriptorId = descriptor.uuid.toKotlinUuid()
        val characteristic = descriptor.characteristic

        when (descriptorId) {
            BLEConstants.CCC_DESCRIPTOR -> launchIfActive {
                Logger.d(tag = TAG) { "WRITE CCC DESCRIPTOR ENABLE/DISABLE SUCCEED ON CHARACTERISTICS:${characteristic.uuid}" }

                // write was successful good now to know what is being written and
                // manage then we read the characteristics
                val bool = gatt.readDescriptor(descriptor)
                Logger.d(tag = TAG) { "READING DESCRIPTOR VALUE STATUS:$bool" }
            }

            else -> {
                Logger.d(tag = TAG) { "DESCRIPTOR READ PERFORMED ON A NON CCC DESCRIPTOR" }
                _onError?.invoke(BLEConnectionException.InvalidCharacteristicsHandlerException())
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        if (gatt == null || characteristic == null) return
        // only use this under API 32
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onCharacteristicChanged(gatt, characteristic, characteristic.value)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        val characteristicId = characteristic.uuid.toKotlinUuid()
        val serviceId = characteristic.service.uuid.toKotlinUuid()
        val deviceAddress = gatt.device.address ?: return

        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> launchIfActive {
                val event = proximityDelegate.handleHandshakeNotification(
                    value = value,
                    onHandshakeSuccess = {
                        // connection handshake success
                        val device = _lock.withLock { _receiverInfo[deviceAddress] }
                        if (device != null) _onEvents?.invoke(ConnectorSyncEvent.HandshakeSuccess(device))

                        gatt.toggleNotification(characteristic, false)
                    },
                )
                if (event.isFailure) {
                    val error = event.exceptionOrNull()
                    _onEvents?.invoke(ConnectorSyncEvent.HandshakeFailed(error?.message))
                    return@launchIfActive
                }
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> launchIfActive {
                val result = syncHandlerDelegate.handleSyncDataNotification(
                    characteristicId = characteristic.uuid.toKotlinUuid(),
                    value = value,
                    onWriteBytes = { bytes -> gatt.writeToCharacteristics(characteristic, bytes) },
                    onToggleNotification = { uuid, enable ->
                        val notificationCharacteristic = gatt.getService(serviceId.toJavaUuid())
                            .getCharacteristic(uuid.toJavaUuid()) ?: return@handleSyncDataNotification false
                        gatt.toggleNotification(notificationCharacteristic, enable)
                    },
                    onEvent = { event -> _onEvents?.invoke(event) },
                    onReadDevice = { _lock.withLock { _receiverInfo[deviceAddress] } },
                )

                result.fold(
                    onSuccess = {},
                    onFailure = { err -> _onError?.invoke(err) },
                )
            }


            else -> {
                Logger.w(tag = TAG) { "NO HANDLER FOR CHARACTERISTIC:$characteristicId and SERVICE:$serviceId" }
                _onError?.invoke(BLEConnectionException.InvalidCharacteristicsHandlerException())
            }
        }
    }

    private fun requestHandshakeCharacteristics(gatt: BluetoothGatt) = runCatching {

        val syncService = gatt.getService(BLEConstants.SYNC_SERVICE_ID.toJavaUuid())
            ?: throw InvalidServiceOrCharacteristicsException(true, BLEConstants.SYNC_SERVICE_ID)

        val characteristic = syncService
            .getCharacteristic(BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID.toJavaUuid())
            ?: throw InvalidServiceOrCharacteristicsException(false, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID)

        val isSuccess = gatt.readCharacteristic(characteristic)
        Logger.d(tag = TAG) { "READ CHARACTERISTICS OP SUCCESS: $isSuccess" }
    }


    private fun onClearCallbacks() {
        Logger.d(tag = TAG) { "CALLBACKS REMOVED" }
        syncHandlerDelegate.cleanUp()
        _onError = null
        _onEvents = null
    }

    private fun onClose() {
        Logger.d(tag = TAG) { "CANCELLING SCOPE IS SCOPE ACTIVE:${_scope?.isActive}" }
        _receiverInfo.clear()
        _scope?.cancel()
        Logger.d(tag = TAG) { "MAKING SURE SCOPE IS SCOPE NOT ACTIVE:${_scope?.isActive == false}" }
        _scope = null
        onClearCallbacks()
    }

    /**
     * Utility to unsure the scope is active for the async s
     */
    private inline fun launchIfActive(crossinline block: suspend CoroutineScope.() -> Unit) {
        val scope = _scope
        if (scope == null || !scope.isActive) {
            Logger.w(tag = TAG) { "COROUTINE SCOPE IS INACTIVE OR NULL" }
            _onError?.invoke(BLEConnectionException.WrongLifecycleRoutineException())
            return
        }
        scope.launch { block() }
    }
}
