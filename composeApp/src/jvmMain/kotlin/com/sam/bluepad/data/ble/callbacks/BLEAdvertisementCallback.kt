package com.sam.bluepad.data.ble.callbacks

import co.touchlab.kermit.Logger
import com.sam.blejavaadvertise.callbacks.GATTServerCallback
import com.sam.blejavaadvertise.models.GATTBluetoothError
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus
import com.sam.bluepad.data.ble.delegate.BLEAdvertiserSyncHandlerDelegate
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

private const val TAG = "BLE_ADVERTISEMENT_CALLBACK"

private typealias NotifyCharacteristicsChanged = (deviceAddress: String, characteristicsUuid: String, value: ByteArray) -> Boolean

class BLEAdvertisementCallback private constructor(
    deviceInfoProvider: LocalDeviceInfoProvider,
    private val externalDevicesRepo: ExternalDevicesRepository,
    private val delegate: BLEAdvertiserSyncHandlerDelegate,
) : GATTServerCallback {

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

    private var _notifyCharacteristicsChanged: NotifyCharacteristicsChanged? = null
    fun setNotifyCharacteristicsChanged(callback: NotifyCharacteristicsChanged) {
        _notifyCharacteristicsChanged = callback
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _peerDevices = MutableStateFlow<List<BLEPeerData>>(emptyList())
    val incomingDeviceData: Flow<List<BLEPeerData>> = _peerDevices.asStateFlow()

    private val _advertiserEvents =
        MutableSharedFlow<AdvertiserSyncEvent>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val syncRequestEvents = _advertiserEvents.asSharedFlow()

    private val _activeSyncDeviceInfo = ConcurrentHashMap<String, ExternalDeviceModel>()

    override fun onServiceAdded(
        serviceUuid: String,
        success: Boolean,
        error: GATTBluetoothError
    ) {
        if (success) Logger.i(tag = TAG) { "SERVICE :$serviceUuid ADDED SUCCESSFULLY" }
        else Logger.i(tag = TAG) { "SERVICE $serviceUuid FAILED ADDED ERROR CODE: $error" }
    }

    override fun onServiceStatusChange(status: GATTServiceAdvertisementStatus?) {
        Logger.d(tag = TAG) { "ADVERTISEMENT STATUS :$status" }
        _isRunning.value = status == GATTServiceAdvertisementStatus.STARTED ||
            status == GATTServiceAdvertisementStatus.STARTED_WITHOUT_ADVERTISEMENT
    }

    override fun onReadCharacteristics(
        deviceAddress: String?,
        serviceUuid: String?,
        characteristicUuid: String?,
    ): ByteArray? {

        if (serviceUuid == null || characteristicUuid == null || deviceAddress == null)
            return null

        val characteristicsId = Uuid.parse(characteristicUuid)
        val serviceId = Uuid.parse(serviceUuid)


        when (characteristicsId) {
            // HANDLE DEVICE DISCOVERY ADVERTISEMENT HERE
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceId == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicsId FROM DISCOVERY SERVICE" }

                val result = delegate.handleDeviceReadRequest(currentDeviceInfo = _deviceInfo.value)
                return result.getOrNull()
            }

            // HANDLE SYNC SERVICE PROXIMITY CHECK ADVERTISEMENT HERE
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicsId FROM SYNC SERVICE" }

                // handshake requested
                _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeStarted)

                val result = runBlocking {
                    delegate.handleProximityReadRequest(
                        address = deviceAddress,
                        currentDevice = _deviceInfo.value,
                    )
                }
                return result.getOrNull()
            }

            else -> {
                Logger.w(tag = TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristicsId} WITH SERVICE:${serviceId}" }
                return null
            }
        }
    }

    override fun onWriteCharacteristicRequest(
        deviceAddress: String?,
        serviceUuid: String?,
        characteristicUuid: String?,
        value: ByteArray?
    ) {
        if (deviceAddress == null || characteristicUuid == null || serviceUuid == null || value == null)
            return

        val characteristicId = Uuid.parse(characteristicUuid)
        val serviceId = Uuid.parse(serviceUuid)

        when (characteristicId) {
            // HANDLE DEVICE DISCOVERY ADVERTISEMENT HERE
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceId == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicId FROM DISCOVERY SERVICE" }

                val result = delegate.handleDeviceWriteRequest(value = value)
                val peerDevice = result.getOrNull() ?: return
                _peerDevices.update { device -> (device + peerDevice).distinctBy { it.deviceId } }
            }

            // HANDLE SYNC AND PROXIMITY SERVICE FROM HERE
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicId FROM SYNC SERVICE" }

                _scope.launch {
                    val result = delegate.handleProximityWriteRequest(
                        value = value,
                        address = deviceAddress,
                        onNotify = { bytes ->
                            _notifyCharacteristicsChanged?.invoke(deviceAddress, characteristicUuid, bytes)
                                ?: false
                        },
                        savedDevices = { id -> externalDevicesRepo.getDeviceByUuid(id) },
                        currentDeviceInfo = _deviceInfo.value,
                    )
                    result.fold(
                        onSuccess = { device ->
                            _activeSyncDeviceInfo[deviceAddress] = device
                            _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeSuccess(device))
                        },
                        onFailure = { err ->
                            _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeFailed("Handshake failed: ${err.message}"))
                        },
                    )
                }
            }

            // HANDLE SYNC AND PROXIMITY SERVICE FROM HERE
            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if (serviceId == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicId FROM SYNC SERVICE" }

                _scope.launch {
                    val result = delegate.handleSyncDataWriteRequest(
                        value = value,
                        onNotify = { bytes ->
                            _notifyCharacteristicsChanged?.invoke(deviceAddress, characteristicUuid, bytes) ?: false
                        },
                    )

                    result.fold(
                        onSuccess = { session ->
                            val device = _activeSyncDeviceInfo[deviceAddress] ?: return@fold
                            val event = when (session) {
                                BLESyncSession.SyncSessionStart -> AdvertiserSyncEvent.SyncStarted(device)
                                BLESyncSession.SyncSessionSuccessful -> AdvertiserSyncEvent.FullDuplexCompleted(device)

                                is BLESyncSession.SyncSessionFailed -> AdvertiserSyncEvent.SyncFailed(session.reason.name)
                                is BLESyncSession.SyncPacketTransition -> {
                                    val isHalfDone =
                                        session.prevType == BLESyncDataType.CONTENT && session.newType == BLESyncDataType.METADATA
                                    if (!isHalfDone) return@fold
                                    // half completed
                                    AdvertiserSyncEvent.HalfDuplexCompleted(device)
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
                }
            }

            else -> {}
        }
    }


    override fun onReadDescriptor(
        deviceAddress: String?,
        serviceUuid: String?,
        characteristicsUuid: String?,
        descriptorUuid: String?
    ): ByteArray? {
        if (deviceAddress == null || descriptorUuid == null || characteristicsUuid == null || serviceUuid == null)
            return null

        Logger.i(tag = TAG) { "READ REQUEST DESCRIPTOR ID $descriptorUuid CHARACTERISTIC ID : $characteristicsUuid" }

        val descriptorId = Uuid.parse(descriptorUuid)
        val characteristicsId = Uuid.parse(characteristicsUuid)
        val serviceId = Uuid.parse(serviceUuid)

        if (serviceId != BLEConstants.SYNC_SERVICE_ID) return null

        when (characteristicsId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> {
                // need to use blocking code otherwise we cannot return the data
                val result = runBlocking {
                    delegate.handleCCCReadRequest(
                        address = deviceAddress,
                        isIndication = true,
                        characteristicsId = characteristicsId,
                        descriptorUuid = descriptorId,
                    )
                }
                return result.getOrNull()
            }

            else -> return null
        }
    }

    override fun onWriteDescriptor(
        deviceAddress: String?,
        serviceUuid: String?,
        characteristicsUuid: String?,
        descriptorUuid: String?,
        value: ByteArray?
    ) {
        if (deviceAddress == null || descriptorUuid == null || value == null || characteristicsUuid == null || serviceUuid == null) return

        Logger.i(tag = TAG) { "WRITE REQUEST DESCRIPTOR ID $descriptorUuid CHARACTERISTIC ID : $characteristicsUuid" }

        val descriptorId = Uuid.parse(descriptorUuid)
        val characteristicId = Uuid.parse(characteristicsUuid)
        val serviceId = Uuid.parse(serviceUuid)

        Logger.d(tag = TAG) { "WRITE REQUEST DESCRIPTOR ID $descriptorId CHARACTERISTIC ID : $characteristicId" }

        // only handle sync service id
        if (serviceId != BLEConstants.SYNC_SERVICE_ID) return

        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> _scope.launch {
                delegate.handleCCCWriteRequest(
                    address = deviceAddress,
                    descriptorUuid = descriptorId,
                    characteristicsId = characteristicId,
                    value = value,
                )
            }

            else -> {}
        }
    }

    fun setRunning(value: Boolean) {
        _isRunning.value = value
    }

    fun cleanUp() {
        // clears everything on done
        if (_scope.isActive) _scope.cancel()
        // clear the maps
        delegate.cleanUp()
        _peerDevices.value = emptyList()
        _activeSyncDeviceInfo.clear()
        // clear the callback ref
        _notifyCharacteristicsChanged = null
    }
}
