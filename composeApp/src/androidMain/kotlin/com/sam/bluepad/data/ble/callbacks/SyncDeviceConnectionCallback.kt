package com.sam.bluepad.data.ble.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.exceptions.GattInvalidStatusException
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.toKotlinUuid

private const val TAG = "SYNC_CONNECTION_CALLBACK"

@SuppressLint("MissingPermission")
class SyncDeviceConnectionCallback private constructor(
    deviceInfoProvider: LocalDeviceInfoProvider,
    private val externalDevicesRepository: ExternalDevicesRepository,
    private val delegate: SyncDeviceConnectionDelegate,
) : BluetoothGattCallback() {

    constructor(
        deviceInfoProvider: LocalDeviceInfoProvider,
        externalDevicesRepository: ExternalDevicesRepository,
        protoBuf: ProtoBuf,
        syncOutPayloadManager: OutPayloadManager,
        syncInPayloadManager: InPayloadManager,
    ) : this(
        deviceInfoProvider = deviceInfoProvider,
        externalDevicesRepository = externalDevicesRepository,
        delegate = SyncDeviceConnectionDelegate(
            protoBuf = protoBuf,
            outPayloadManager = syncOutPayloadManager,
            inPayloadManager = syncInPayloadManager,
        ),
    )

    private val _scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var _onEvents: ((ConnectorSyncEvent) -> Unit)? = null
    private var _onError: ((Throwable) -> Unit)? = null

    fun onEvents(callback: (ConnectorSyncEvent) -> Unit) {
        _onEvents = callback
    }

    fun onError(callback: (Throwable) -> Unit) {
        _onError = callback
    }

    private val _currentDeviceProfile = deviceInfoProvider.readDeviceInfo
        .stateIn(
            scope = _scope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

//	private val _savedExternalDevices = devicesRepository.getAllDevices()
//		.onEach { res ->
//			if (res !is Resource.Error) return@onEach
//			Logger.e(TAG, res.error) { "SOME ERROR OCCURRED WHILE READING DEVICES" }
//		}
//		.filterIsInstance<Resource.Success<List<ExternalDeviceModel>, Exception>>()
//		.map { res -> res.data }
//		.stateIn(
//			scope = _scope,
//			started = SharingStarted.Eagerly,
//			initialValue = emptyList(),
//		)


    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT READ CONNECTION STATE" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }
        val deviceAddress = gatt?.device?.address
        when (newState) {
            // device connected
            BluetoothGatt.STATE_CONNECTED -> {
                _onEvents?.invoke(ConnectorSyncEvent.ConnectionSuccess)
                Logger.i(TAG) { "DEVICE:$deviceAddress CONNECTED" }

                val isMtuRequested = gatt?.requestMtu(BLEConstants.REQUESTED_MTU) ?: false
                val isDiscoveryRequested = gatt?.discoverServices() ?: false
                Logger.d(TAG) { "REQUESTED SERVICE DISCOVERY STARTED:$isDiscoveryRequested" }
                Logger.d(TAG) { "REQUESTING HIGHER MTU: ${BLEConstants.REQUESTED_MTU} STATUS:$isMtuRequested" }
            }

            // device disconnected
            BluetoothGatt.STATE_DISCONNECTED -> {
                Logger.i(TAG) { "DEVICE:$deviceAddress DIS_CONNECTED" }
                _onEvents?.invoke(ConnectorSyncEvent.DeviceDisconnected)
            }

            else -> {}
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (gatt == null) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT DISCOVER SERVICES" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }
        val result = delegate.requestHandshakeCharacteristics(gatt = gatt)
        if (result.isSuccess) {
            _onEvents?.invoke(ConnectorSyncEvent.ServicesDiscovered)
        } else {
            val error = result.exceptionOrNull() ?: return
            _onError?.invoke(error)
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT UPDATE MTU" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }
        Logger.d(TAG) { "GATT CONNECTION MTU UPDATED :$mtu" }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onCharacteristicRead(gatt, characteristic, characteristic.value, status)
    }


    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT READ CHARACTERISTIC : ${characteristic.uuid}" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }

        val characteristicsId = characteristic.uuid.toKotlinUuid()
        val serviceId = characteristic.service.uuid.toKotlinUuid()

        when (characteristicsId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if serviceId == BLEConstants.SYNC_SERVICE_ID -> {
                _scope.launch {
                    val result = delegate.handleHandshakeRead(
                        gatt = gatt,
                        characteristic = characteristic,
                        value = value,
                        deviceInfo = _currentDeviceProfile.value,
                        savedDevices = { id -> externalDevicesRepository.getDeviceByUuid(id) },
                    )
                    if (result.isSuccess) {
                        val model = result.getOrThrow()
                        _onEvents?.invoke(ConnectorSyncEvent.AdvertisingDeviceRead(model))
                    } else {
                        val error = result.exceptionOrNull() ?: return@launch
                        _onError?.invoke(error)
                    }
                }
            }

            else -> Logger.w(TAG) {
                "NO READ METHOD PRESENT FOR CHARACTERISTICS:$characteristicsId SERVICE:$serviceId"
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
            Logger.w(TAG) { "WRITE RESPONSE FAILED" }
            _onError?.invoke(GattInvalidStatusException(status))
            return
        }

        val characteristicsId = characteristic.uuid.toKotlinUuid()
        val serviceId = characteristic.service.uuid.toKotlinUuid()

        when (characteristicsId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if serviceId == BLEConstants.SYNC_SERVICE_ID -> {
                _onEvents?.invoke(ConnectorSyncEvent.ConnectorDeviceDataResponseSend)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if serviceId == BLEConstants.SYNC_SERVICE_ID -> return

            else -> Logger.w(TAG) { "NO WRITE RESPONSE EXCEPTED FROM CHARACTERISTICS:$characteristicsId SERVICE:$serviceId" }
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
        onDescriptorRead(gatt, descriptor, status, descriptor.value)
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray,
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "DESCRIPTOR READ FAILED STATUS CODE:$status" }
            return
        }
        val descriptorId = descriptor.uuid.toKotlinUuid()
        val characteristic = descriptor.characteristic

        when (descriptorId) {
            BLEConstants.CCC_DESCRIPTOR -> delegate.onEnabledDisabledCCCDescriptor(gatt, characteristic, value)
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int,
    ) {
        if (gatt == null || descriptor == null) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "DESCRIPTOR WRITE FAILED STATUS: $status" }
            return
        }

        val descriptorId = descriptor.uuid.toKotlinUuid()
        val characteristic = descriptor.characteristic

        when (descriptorId) {
            BLEConstants.CCC_DESCRIPTOR -> {
                Logger.d(TAG) { "WRITE CCC DESCRIPTOR ENABLE/DISABLE SUCCEED ON CHARACTERISTICS:${characteristic.uuid}" }
                // write was successful good now to know what is being written and
                // manage then we read the characteristics
                gatt.readDescriptor(descriptor)
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

        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                val event = delegate
                    .handleHandshakeNotification(gatt = gatt, characteristic = characteristic, value = value)
                    .getOrElse { err ->
                        _onError?.invoke(err)
                        return
                    }
                _onEvents?.invoke(event)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                if (!_scope.isActive) {
                    Logger.w(TAG) { "COROUTINE IS NOT ACTIVE" }
                    return
                }

                _scope.launch {
                    delegate.handleSyncDataNotification(
                        gatt = gatt,
                        characteristic = characteristic,
                        value = value,
                        onEvent = { event -> _onEvents?.invoke(event) },
                        onError = { exp -> if (exp is Exception) _onError?.invoke(exp) },
                    )
                }
            }

            else -> Logger.w(TAG) {
                "NO HANDLER FOR CHARACTERISTIC:$characteristicId and SERVICE:$serviceId"
            }
        }
    }

    fun onClearCallbacks() {
        Logger.d(TAG) { "CALLBACKS REMOVED" }
        _onError = null
        _onEvents = null
    }

    fun onClose() {
        Logger.d(TAG) { "CANCELLING SCOPE IS SCOPE ACTIVE:${_scope.isActive}" }
        _scope.cancel()
        onClearCallbacks()
    }
}
