package com.sam.bluepad.data.ble.callbacks

import co.touchlab.kermit.Logger
import com.sam.ble_advertise.BLEAdvertiserListener
import com.sam.ble_advertise.models.BLEAdvertisementStatus
import com.sam.ble_advertise.models.GattWriteResponse
import com.sam.bluepad.data.ble.delegate.BLEAdvertiserSyncHandlerDelegate
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

private const val TAG = "BLE_ADVERTISEMENT_CALLBACK"

private typealias NotifyCharacteristicsChanged = (deviceAddress: String, characteristicsUuid: Uuid, value: ByteArray) -> Boolean

class BLEAdvertisementCallback private constructor(
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val externalDevicesRepo: ExternalDevicesRepository,
    private val delegate: BLEAdvertiserSyncHandlerDelegate,
) : BLEAdvertiserListener {

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

    private suspend fun readLocalDevice(): LocalDeviceInfoModel {
        return deviceInfoProvider.readDeviceInfo.first()
    }

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

    override suspend fun onServiceAdded(serviceUuid: Uuid, success: Boolean, errorCode: Int) {
        if (success) Logger.i(tag = TAG) { "SERVICE :$serviceUuid ADDED SUCCESSFULLY" }
        else Logger.i(tag = TAG) { "SERVICE $serviceUuid FAILED ADDED ERROR CODE: $errorCode" }
    }

    override suspend fun onServiceStatusChange(status: BLEAdvertisementStatus) {
        Logger.d(tag = TAG) { "ADVERTISEMENT STATUS :$status" }
        _isRunning.value = status == BLEAdvertisementStatus.Started ||
            status == BLEAdvertisementStatus.StartedWithoutAdvertisementData
    }

    override suspend fun onReadCharacteristic(
        deviceAddress: String,
        serviceUuid: Uuid,
        characteristicUuid: Uuid
    ): ByteArray? {
        when (characteristicUuid) {
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceUuid == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM DISCOVERY SERVICE" }
                val device = readLocalDevice()
                val result = delegate.handleDeviceReadRequest(currentDeviceInfo = device)
                return result.getOrNull()
            }

            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceUuid == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM SYNC SERVICE" }
                _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeStarted)
                val device = readLocalDevice()
                val result =
                    delegate.handleProximityReadRequest(address = deviceAddress, currentDevice = device)

                return result.getOrNull()
            }

            else -> {
                Logger.w(tag = TAG) { "CANNOT FIND ANY CHARACTERISTICS:${characteristicUuid} WITH SERVICE:${serviceUuid}" }
                return null
            }
        }
    }

    override suspend fun onWriteCharacteristic(
        address: String,
        serviceUuid: Uuid,
        characteristicUuid: Uuid,
        value: ByteArray,
    ): GattWriteResponse {
        when (characteristicUuid) {
            BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID if (serviceUuid == BLEConstants.DEVICE_INFO_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM DISCOVERY SERVICE" }
                val result = delegate.handleDeviceWriteRequest(value = value)
                val peerDevice = result.getOrNull() ?: return GattWriteResponse.FAILED
                _peerDevices.update { device -> (device + peerDevice).distinctBy { it.deviceId } }
                return GattWriteResponse.SUCCESS
            }

            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if (serviceUuid == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM SYNC SERVICE" }

                val device = readLocalDevice()

                val result = delegate.handleProximityWriteRequest(
                    value = value,
                    address = address,
                    onNotify = { bytes ->
                        _notifyCharacteristicsChanged
                            ?.invoke(address, characteristicUuid, bytes)
                            ?: false
                    },
                    savedDevices = { id -> externalDevicesRepo.getDeviceByUuid(id) },
                    currentDeviceInfo = device,
                )
                result.fold(
                    onSuccess = { device ->
                        _activeSyncDeviceInfo[address] = device
                        _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeSuccess(device))
                        return GattWriteResponse.SUCCESS
                    },
                    onFailure = { err ->
                        _advertiserEvents.tryEmit(AdvertiserSyncEvent.HandshakeFailed("Handshake failed: ${err.message}"))
                    },
                )
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if (serviceUuid == BLEConstants.SYNC_SERVICE_ID) -> {
                Logger.d(tag = TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM SYNC SERVICE" }

                val result = delegate.handleSyncDataWriteRequest(
                    value = value,
                    onNotify = { bytes ->
                        _notifyCharacteristicsChanged?.invoke(address, characteristicUuid, bytes) ?: false
                    },
                )

                result.fold(
                    onSuccess = { session ->
                        val device = _activeSyncDeviceInfo[address]
                        if (device != null) {
                            val event = when (session) {
                                is BLESyncSession.SyncSessionStart -> AdvertiserSyncEvent.SyncStarted(device)
                                is BLESyncSession.SyncSessionSuccessful ->
                                    AdvertiserSyncEvent.FullDuplexCompleted(device, session.sessionId)

                                is BLESyncSession.SyncSessionFailed -> AdvertiserSyncEvent.SyncFailed(session.reason.name)
                                is BLESyncSession.SyncPacketTransition -> {
                                    val isHalfDone =
                                        session.prevType == BLESyncDataType.CONTENT && session.newType == BLESyncDataType.METADATA
                                    if (isHalfDone) AdvertiserSyncEvent.HalfDuplexCompleted(device) else null
                                }

                                else -> null
                            }
                            if (event != null) _advertiserEvents.tryEmit(event)
                        }
                        return GattWriteResponse.SUCCESS
                    },
                    onFailure = { error ->
                        val event = AdvertiserSyncEvent.SyncFailed(error.message ?: "Unknown Error")
                        _advertiserEvents.tryEmit(event)
                    },
                )
            }

            else -> {}
        }

        return GattWriteResponse.FAILED
    }

    override suspend fun onReadDescriptor(
        address: String,
        serviceUuid: Uuid,
        characteristicUuid: Uuid,
        descriptorUuid: Uuid,
        status: Int,
    ): ByteArray? {
        Logger.i(tag = TAG) { "READ REQUEST DESCRIPTOR ID $descriptorUuid CHARACTERISTIC ID : $characteristicUuid" }
        if (serviceUuid != BLEConstants.SYNC_SERVICE_ID) return null

        when (characteristicUuid) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> {
                val result = delegate.handleCCCReadRequest(
                    address = address,
                    isIndication = true,
                    characteristicsId = characteristicUuid,
                    descriptorUuid = descriptorUuid,
                )

                return result.getOrNull()
            }

            else -> {}
        }
        return null
    }

    override suspend fun onWriteDescriptor(
        address: String,
        serviceUuid: Uuid,
        characteristicUuid: Uuid,
        descriptorUuid: Uuid,
        value: ByteArray,
    ): GattWriteResponse {

        Logger.i(tag = TAG) { "WRITE REQUEST DESCRIPTOR ID $descriptorUuid CHARACTERISTIC ID : $characteristicUuid" }

        if (serviceUuid != BLEConstants.SYNC_SERVICE_ID) {
            return GattWriteResponse.FAILED
        }

        when (characteristicUuid) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID, BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> {

                val result = delegate.handleCCCWriteRequest(
                    address = address,
                    characteristicsId = characteristicUuid,
                    descriptorUuid = descriptorUuid,
                    value = value,
                )
                if (result.isSuccess) return GattWriteResponse.SUCCESS
            }

            else -> {}
        }
        return GattWriteResponse.FAILED
    }

    override suspend fun onIndicationResult(
        address: String,
        characteristicUuid: Uuid,
        success: Boolean,
        status: Int,
        errorCode: Int
    ) {

    }

    fun setRunning(value: Boolean) {
        _isRunning.value = value
    }

    fun cleanUp() {
        delegate.cleanUp()
        _peerDevices.value = emptyList()
        _activeSyncDeviceInfo.clear()
        _notifyCharacteristicsChanged = null
    }
}
