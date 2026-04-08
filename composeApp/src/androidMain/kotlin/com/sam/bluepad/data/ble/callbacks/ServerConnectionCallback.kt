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
import com.sam.bluepad.data.ble.delegate.BLEAdvertiserSyncHandlerDelegate
import com.sam.bluepad.data.ble.exceptions.GattInvalidCharacteristicsException
import com.sam.bluepad.data.ble.exceptions.GattInvalidDescriptorException
import com.sam.bluepad.data.ble.utils.hasIndication
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.toKotlinUuid

private const val TAG = "SERVER_CALLBACK"

private typealias GATTSendResponse = (device: BluetoothDevice?, requestId: Int, status: Int, offset: Int, value: ByteArray?) -> Unit
private typealias GATTNotifyCharacteristicsChanged = (device: BluetoothDevice, characteristics: BluetoothGattCharacteristic, value: ByteArray) -> Boolean

@SuppressLint("MissingPermission")
class ServerConnectionCallback private constructor(
    deviceInfoProvider: LocalDeviceInfoProvider,
    private val externalDevicesRepo: ExternalDevicesRepository,
    private val delegate: BLEAdvertiserSyncHandlerDelegate,
) : BluetoothGattServerCallback() {

    constructor(
        protoBuf: ProtoBuf,
        randomGenerator: RandomGenerator,
        platformInfoProvider: PlatformInfoProvider,
        encoder: BytesEncoder,
        deviceInfoProvider: LocalDeviceInfoProvider,
        externalDevicesRepo: ExternalDevicesRepository,
        syncInManager: InPayloadManager,
        syncOutManager: OutPayloadManager,
    ) : this(
        deviceInfoProvider = deviceInfoProvider,
        externalDevicesRepo = externalDevicesRepo,
        delegate = BLEAdvertiserSyncHandlerDelegate(
            protoBuf = protoBuf,
            randomGenerator = randomGenerator,
            platformInfoProvider = platformInfoProvider,
            encoder = encoder,
            inPayloadManager = syncInManager,
            outPayloadManager = syncOutManager,
        ),
    )

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _deviceInfo = deviceInfoProvider.readDeviceInfo.stateIn(
        scope = _scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    private var _onCharacteristicsChanged: GATTNotifyCharacteristicsChanged? = null
    private var _sendResponse: GATTSendResponse? = null
    private var _onServiceAdded: (() -> Unit)? = null

    fun setOnSendResponse(callback: GATTSendResponse) {
        _sendResponse = callback
    }

    fun setNotifyCharacteristicsChanged(callback: GATTNotifyCharacteristicsChanged) {
        _onCharacteristicsChanged = callback
    }

    fun setOnServiceAdded(onServiceAdded: () -> Unit = {}) {
        _onServiceAdded = onServiceAdded
    }

    private val _activeSyncDeviceInfo = ConcurrentHashMap<String, ExternalDeviceModel>()

    private val _peerDevices = MutableStateFlow<List<BLEPeerData>>(emptyList())
    val peerDevices = _peerDevices.asStateFlow()

    private val _advertiserEvents =
        MutableSharedFlow<AdvertiserSyncEvent>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val syncRequestEvents = _advertiserEvents.asSharedFlow()

    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        if (device == null || status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "CONNECTION STATE FAILED CODE:$status" }
            return
        }

        val bondState = try {
            when (device.bondState) {
                BluetoothDevice.BOND_NONE -> "NO BOND"
                BluetoothDevice.BOND_BONDED -> "BONDED"
                BluetoothDevice.BOND_BONDING -> "BONDING"
                else -> null
            }
        } catch (e: SecurityException) {
            Logger.e(tag = TAG, throwable = e) { "MISSING CONNECT PERMISSION" }
            null
        }


        val state = when (newState) {
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
            else -> null
        }
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            delegate.markDeviceDisconnectedAndClearCache(device.address)
        }
        Logger.i(tag = TAG) { "DEVICE IDENTIFIER:${device.address} BOND STATE:$bondState CONNECTION_STATE:$state" }
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
        if (device == null) return
        Logger.d(tag = TAG) { "DEVICE IDENTIFIER:${device.address} NEW MAX_TRANSMISSION_UNIT:$mtu" }
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(tag = TAG) { "SOME ERROR IN ADDING THE SERVICE_ID:${service?.uuid} GATT_STATUS:$status" }
            return
        }
        if (service == null) return
        Logger.i(tag = TAG) { "SERVICE :${service.uuid} ADDED" }
        _onServiceAdded?.invoke()
    }

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        characteristic: BluetoothGattCharacteristic?,
    ) {
        if (device == null || characteristic == null) {
            Logger.w(tag = TAG) { "CANNOT READ DEVICE OR CHARACTERISTICS" }
            return
        }

        val serviceId = characteristic.service.uuid.toKotlinUuid()
        val characteristicsId = characteristic.uuid.toKotlinUuid()

        when (characteristicsId) {
            // HANDLE DEVICE DISCOVERY ADVERTISEMENT HERE
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceId == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }

                val result = delegate.handleDeviceReadRequest(currentDeviceInfo = _deviceInfo.value)
                sendReadResponse(device, requestId, offset, result)
                return
            }

            // HANDLE SYNC SERVICE PROXIMITY CHECK ADVERTISEMENT HERE
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> _scope.launch {
                Logger.d(tag = TAG) { "READ REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }

                // handshake requested
                _advertiserEvents.tryEmit(AdvertiserSyncEvent.IncomingHandshakeRequest)

                val result = delegate.handleProximityReadRequest(
                    address = device.address,
                    currentDevice = _deviceInfo.value,
                )
                sendReadResponse(device, requestId, offset, result)
            }

            else -> {
                val failedResult =
                    Result.failure<ByteArray>(GattInvalidCharacteristicsException(characteristic))
                Logger.w(tag = TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}" }
                sendReadResponse(device, requestId, offset, failedResult)
                return
            }
        }
    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?,
    ) {
        // SOME DATA IS PRESENT
        if (device == null || characteristic == null || value == null) {
            Logger.w(tag = TAG) { "CANNOT READ DEVICE OR CHARACTERISTICS" }
            return
        }

        val characteristicId = characteristic.uuid.toKotlinUuid()
        val serviceId = characteristic.service.uuid.toKotlinUuid()

        when (characteristicId) {
            // HANDLE DEVICE DISCOVERY ADVERTISEMENT HERE
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceId == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM DISCOVERY SERVICE" }

                val result = delegate.handleDeviceWriteRequest(value = value)
                val peerDevice = result.getOrElse { err ->
                    val errorResult = Result.failure<Unit>(err)
                    sendWriteResponse(device, requestId, offset, responseNeeded, value, errorResult)
                    return
                }
                _peerDevices.update { device -> (device + peerDevice).distinctBy { it.deviceId } }
            }

            // HANDLE SYNC AND PROXIMITY SERVICE FROM HERE
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }

                _scope.launch {
                    val result = delegate.handleProximityWriteRequest(
                        value = value,
                        address = device.address,
                        onNotify = { bytes ->
                            _onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
                        },
                        savedDevices = { id -> externalDevicesRepo.getDeviceByUuid(id) },
                        currentDeviceInfo = _deviceInfo.value,
                    )
                    result.fold(
                        onSuccess = { externalDevice ->
                            _activeSyncDeviceInfo[device.address] = externalDevice
                            _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeSuccess(externalDevice))
                        },
                        onFailure = { err ->
                            _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeFailed("Handshake: ${err.message}"))
                        },
                    )
                    // send response
                    sendWriteResponse(device, requestId, offset, responseNeeded, value, result.toUnitResult())
                }
            }

            // HANDLE SYNC AND PROXIMITY SERVICE FROM HERE
            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : ${characteristic.uuid} FROM SYNC SERVICE" }

                _scope.launch {
                    val result = delegate.handleSyncDataWriteRequest(
                        value = value,
                        onNotify = { bytes ->
                            _onCharacteristicsChanged?.invoke(device, characteristic, bytes) ?: false
                        },
                    )
                    result.fold(
                        onSuccess = { session ->
                            val device = _activeSyncDeviceInfo[device.address] ?: return@fold
                            val event = when (session) {
                                BLESyncSession.SyncSessionStart -> AdvertiserSyncEvent.SyncStarted(device)
                                BLESyncSession.SyncSessionSuccessful ->
                                    AdvertiserSyncEvent.SyncCompleted(device, isFull = true)

                                is BLESyncSession.SyncSessionFailed -> AdvertiserSyncEvent.SyncFailed(session.reason.name)
                                is BLESyncSession.SyncPacketTransition -> {
                                    val isHalfDone =
                                        session.prevType == BLESyncDataType.CONTENT && session.newType == BLESyncDataType.METADATA
                                    if (!isHalfDone) return@fold
                                    // half completed
                                    AdvertiserSyncEvent.SyncCompleted(device, isFull = false)
                                }

                                else -> return@fold
                            }
                            _advertiserEvents.tryEmit(event)
                        },
                        onFailure = { error ->
                            val event = AdvertiserSyncEvent.SyncFailed(error.message ?: "Unknown Error")
                            _advertiserEvents.tryEmit(event)
                        },
                    )
                    // send write response
                    sendWriteResponse(device, requestId, offset, responseNeeded, value, result.toUnitResult())
                }
            }

            else -> {
                val res = Result.failure<Unit>(GattInvalidCharacteristicsException(characteristic))
                sendWriteResponse(device, requestId, offset, responseNeeded, value, res)
            }
        }
    }

    override fun onDescriptorReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        descriptor: BluetoothGattDescriptor?,
    ) {
        if (device == null || descriptor == null) {
            Logger.w(tag = TAG) { "CANNOT READ DEVICE OR DESCRIPTOR" }
            return
        }

        Logger.i(tag = TAG) { "READ REQUEST DESCRIPTOR ID ${descriptor.uuid} CHARACTERISTIC ID : ${descriptor.characteristic.uuid}" }

        val serviceId = descriptor.characteristic.service.uuid.toKotlinUuid()
        val characteristicsId = descriptor.characteristic.uuid.toKotlinUuid()
        val descriptorId = descriptor.uuid.toKotlinUuid()

        // only handle sync service id
        if (serviceId != BLEConstants.SYNC_SERVICE_ID) return

        when (characteristicsId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> _scope.launch {
                val result = delegate.handleCCCReadRequest(
                    address = device.address,
                    descriptorUuid = descriptorId,
                    characteristicsId = characteristicsId,
                    isIndication = descriptor.characteristic.hasIndication,
                )
                sendReadResponse(device, requestId, offset, result)
            }

            else -> {
                sendReadResponse(
                    device, requestId, offset,
                    Result.failure(GattInvalidDescriptorException(descriptor)),
                )
            }
        }
    }

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        descriptor: BluetoothGattDescriptor?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?,
    ) {
        if (device == null || descriptor == null || value == null) {
            Logger.w(tag = TAG) { "CANNOT READ DEVICE OR DESCRIPTOR" }
            return

        }

        val serviceId = descriptor.characteristic.service.uuid.toKotlinUuid()
        val characteristicId = descriptor.characteristic.uuid.toKotlinUuid()
        val descriptorId = descriptor.uuid.toKotlinUuid()
        Logger.d(tag = TAG) { "WRITE REQUEST DESCRIPTOR ID $descriptorId CHARACTERISTIC ID : $characteristicId" }

        // only handle sync service id
        if (serviceId != BLEConstants.SYNC_SERVICE_ID) {
            sendWriteResponse(
                device, requestId, offset, responseNeeded, value,
                Result.failure(GattInvalidDescriptorException(descriptor)),
            )
            return
        }

        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> _scope.launch {
                val result = delegate.handleCCCWriteRequest(
                    address = device.address,
                    descriptorUuid = descriptorId,
                    characteristicsId = characteristicId,
                    value = value,
                )
                sendWriteResponse(device, requestId, offset, responseNeeded, value, result)
            }

            else -> {
                sendWriteResponse(
                    device, requestId, offset, responseNeeded, value,
                    Result.failure(GattInvalidDescriptorException(descriptor)),
                )
                return
            }
        }
    }

    private fun sendReadResponse(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        result: Result<ByteArray>,
    ) {
        if (result.isFailure) {
            val reason = result.exceptionOrNull()?.message
            reason?.let { Logger.w(tag = TAG) { "SENDING GATT READ FAILURE REASON:$it" } }
            _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
            return
        }
        // send ok
        val value = result.getOrNull()
        Logger.d(tag = TAG) { "SENDING GATT READ SUCCESS RESPONSE" }
        _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
    }

    private fun sendWriteResponse(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        responseNeeded: Boolean,
        value: ByteArray?,
        result: Result<Unit>,
    ) {
        if (!responseNeeded) return

        if (result.isFailure) {
            val reason = result.exceptionOrNull()?.message
            reason?.let { Logger.w(tag = TAG) { "SENDING GATT WRITE FAILED MESSAGE:$it" } }
            _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
            return
        }
        // send ok
        Logger.d(tag = TAG) { "SENDING GATT WRITE SUCCESS RESPONSE" }
        _sendResponse?.invoke(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
    }

    fun cleanUp() {
        // clears everything on done
        if (_scope.isActive) _scope.cancel()
        // clear the maps
        delegate.cleanUp()
        _peerDevices.value = emptyList()

        // clean the callbacks too
        _sendResponse = null
        _onCharacteristicsChanged = null
        _onServiceAdded = null
    }

    private fun Result<Any>.toUnitResult(): Result<Unit> = map { }
}
