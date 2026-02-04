package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.blejavaadvertise.callbacks.GATTServerCallback
import com.sam.blejavaadvertise.models.GATTBluetoothError
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.ble.models.BLEServerSyncEvent
import com.sam.bluepad.domain.ble.models.BLESyncACKFailedReason
import com.sam.bluepad.domain.ble.models.BLESyncData
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

private const val NONCE_SIZE = 8
private const val TAG = "BLE_ADVERTISEMENT_CALLBACK"

private typealias NotifyCharacteristicsChanged = (deviceAddress: String, characteristicsUuid: String, confirm: Boolean, value: ByteArray) -> Unit

class BLEAdvertisementCallback(
    localDeviceProvider: LocalDeviceInfoProvider,
    externalDevicesRepository: ExternalDevicesRepository,
    private val protoBuf: ProtoBuf,
    private val encoder: BytesEncoder,
    private val randomGenerator: RandomGenerator,
    private val platformInfoProvider: PlatformInfoProvider,
) : GATTServerCallback {

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _localNonceMap = ConcurrentHashMap<String, String>()
    private val _cccDescriptorMap = ConcurrentHashMap<String, Boolean>()

    private val _deviceInfo = localDeviceProvider.readDeviceInfo
        .stateIn(_scope, SharingStarted.Eagerly, null)

    private val _savedDevices = externalDevicesRepository.getAllDevices()
        .onEach { res ->
            if (res !is Resource.Error) return@onEach
            Logger.e(TAG, res.error) { "SOME ERROR OCCURRED WHILE READING DEVICES" }
        }
        .filterIsInstance<Resource.Success<List<ExternalDeviceModel>, Exception>>()
        .map { res -> res.data }
        .stateIn(_scope, SharingStarted.Eagerly, emptyList())

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _incomingDeviceData = MutableStateFlow<List<BLEPeerData>>(emptyList())
    val incomingDeviceData: Flow<List<BLEPeerData>>
        get() = _incomingDeviceData.asStateFlow()

    private val _incomingSyncRequest = Channel<BLEServerSyncEvent>(Channel.CONFLATED)
    val syncRequestEvents: Flow<BLEServerSyncEvent>
        get() = _incomingSyncRequest.receiveAsFlow()

    private var _notifyCharacteristicsChanged: NotifyCharacteristicsChanged? = null

    fun setNotifyCharacteristicsChanged(callback: NotifyCharacteristicsChanged) {
        _notifyCharacteristicsChanged = callback
    }

    override fun onServiceAdded(
        serviceUuid: String,
        success: Boolean,
        error: GATTBluetoothError
    ) {
        if (success) Logger.i(TAG) { "SERVICE :$serviceUuid ADDED SUCCESSFULLY" }
        else Logger.i(TAG) { "SERVICE $serviceUuid FAILED ADDED ERROR CODE: $error" }
    }

    override fun onServiceStatusChange(status: GATTServiceAdvertisementStatus?) {
        Logger.d(TAG) { "ADVERTISEMENT STATUS :$status" }
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

        val characteristicsUUID = Uuid.parse(characteristicUuid)
        val serviceUUID = Uuid.parse(serviceUuid)

        if (serviceUUID == BLEConstants.DEVICE_INFO_SERVICE_ID) {
            if (characteristicsUUID != BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID) return null

            Logger.d(TAG) { "READ REQUEST FOR DEVICE DISCOVERY CHARACTERISTIC_ID : $characteristicsUUID" }

            val deviceInfo = _deviceInfo.value ?: run {
                Logger.w(TAG) { "UNABLE TO READ CURRENT DEVICE INFO" }
                return null
            }

            val value = try {
                val nonce = randomGenerator.generateRandomBytes(NONCE_SIZE)
                val peerData = BLEPeerData(
                    deviceId = deviceInfo.deviceId,
                    deviceName = deviceInfo.name,
                    nonce = encoder.encodeBytes(nonce),
                    deviceOs = platformInfoProvider.platformOS,
                )
                protoBuf.encodeToByteArray<BLEPeerData>(peerData)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                null
            }

            Logger.d(TAG) { "SENDING READ RESPONSE FOR CHARACTERISTICS_ID :${characteristicUuid}" }
            return value
        }
        // handle the sync service here
        if (serviceUUID == BLEConstants.SYNC_SERVICE_ID) {
            if (characteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) return null

            Logger.d(TAG) { "READ REQUEST FOR SYNC DEVICE CHARACTERISTICS_ID: $characteristicUuid" }
            val deviceInfo = _deviceInfo.value ?: run {
                Logger.w(TAG) { "UNABLE TO READ CURRENT DEVICE INFO" }
                return null
            }

            val value = try {
                val nonce = randomGenerator.generateRandomBytes(NONCE_SIZE)
                    .let { nonceBytes -> encoder.encodeBytes(nonceBytes) }
                    .apply { _localNonceMap[deviceAddress] = this }

                val peerData = BLESyncData.BLEAdvertiseData(
                    deviceId = deviceInfo.deviceId,
                    nonce = nonce,
                    allowSync = true
                )
                val bytes = protoBuf.encodeToByteArray<BLESyncData.BLEAdvertiseData>(peerData)
                Logger.d(TAG) { "SENDING READ RESPONSE FOR CHARACTERISTICS_ID :${characteristicUuid}" }
                bytes
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                null
            }
            return value
        }
        Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
        return null
    }

    override fun onWriteCharacteristicRequest(
        deviceAddress: String?,
        serviceUuid: String?,
        characteristicUuid: String?,
        value: ByteArray?
    ) {
        if (deviceAddress == null || characteristicUuid == null || serviceUuid == null || value == null)
            return

        val characteristicsUUID = Uuid.parse(characteristicUuid)
        val serviceUUID = Uuid.parse(serviceUuid)
        Logger.d(TAG) { "WRITE REQUESTED ON :$characteristicUuid SERVICE:$serviceUuid" }

        if (serviceUUID == BLEConstants.DEVICE_INFO_SERVICE_ID) {
            if (characteristicsUUID != BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID) return
            Logger.d(TAG) { "WRITE REQUEST FOR DEVICE SERVICE CHARACTERISTIC_ID : $characteristicUuid" }
            val peerData = try {
                protoBuf.decodeFromByteArray<BLEPeerData>(value)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                return
            }
            _incomingDeviceData.update { devices -> (devices + peerData).distinctBy { it.deviceId } }
        }

        // HANDLE SYNC SERVICE ID
        if (serviceUUID == BLEConstants.SYNC_SERVICE_ID) {
            if (characteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) return
            Logger.d(TAG) { "WRITE REQUEST FOR SYNC SERVICE CHARACTERISTIC_ID : $characteristicUuid" }
            try {
                val response = protoBuf.decodeFromByteArray<BLESyncData.BLEAdvertiseResponse>(value)
                Logger.d(TAG) { "WRITE REQUESTED ACCEPTED CHARACTERISTIC ID: $characteristicUuid" }

                val savedNonce = _localNonceMap[deviceAddress]
                val externalDevice = _savedDevices.value.find { it.id == response.senderID }
                val connectionId = Uuid.random()

                val data = when {
                    savedNonce == null -> BLESyncData.BLESyncACKFailed(reason = BLESyncACKFailedReason.INVALID_INCOMING_DATA)
                    savedNonce != response.nonce ->
                        BLESyncData.BLESyncACKFailed(BLESyncACKFailedReason.TAMPERED_DATA)

                    response.receiverID != _deviceInfo.value?.deviceId ->
                        BLESyncData.BLESyncACKFailed(reason = BLESyncACKFailedReason.INVALID_RECEIVER)

                    externalDevice == null -> BLESyncData.BLESyncACKFailed(BLESyncACKFailedReason.UNKNOWN_SENDER)
                    else -> BLESyncData.BLESyncACKSuccess(
                        serverID = connectionId,
                        nonce = response.nonce,
                        deviceAddress = deviceAddress
                    )
                }

                val bytes = protoBuf.encodeToByteArray<BLESyncData>(data)
                _notifyCharacteristicsChanged?.invoke(
                    deviceAddress,
                    characteristicUuid,
                    false,
                    bytes
                )

                if (externalDevice == null) return
                val event = BLEServerSyncEvent.SyncRequest(externalDevice, connectionId)
                _incomingSyncRequest.trySend(event)
                when (data) {
                    is BLESyncData.BLESyncACKSuccess -> Logger.d(TAG) { "NOTIFICATION ON CHARACTERISTIC : $characteristicUuid SEND IS_SUCCESS:UNKOWN" }
                    is BLESyncData.BLESyncACKFailed -> Logger.d(TAG) { "NOTIFICATION ON CHARACTERISTIC : $characteristicUuid FAILED REASON :${data.reason}" }
                    else -> {}
                }
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
            }
            return
        }
        // invalids
        Logger.w(TAG) { "REQUESTING INVALID SERVICE" }
    }

    override fun onReadDescriptor(
        deviceAddress: String?,
        serviceUuid: String?,
        characteristicsUuid: String?,
        descriptorUuid: String?
    ): ByteArray? {
        if (deviceAddress == null || descriptorUuid == null || characteristicsUuid == null) return null

        Logger.i(TAG) { "READ REQUEST DESCRIPTOR ID $descriptorUuid CHARACTERISTIC ID : $characteristicsUuid" }

        val parsedDescriptorUUID = Uuid.parse(descriptorUuid)
        val parsedCharacteristicsUUID = Uuid.parse(characteristicsUuid)

        if (parsedDescriptorUUID != BLEConstants.CCC_DESCRIPTOR) {
            Logger.d(TAG) { "INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED" }
            return null
        }
        if (parsedCharacteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) {
            Logger.d(TAG) { "INVALID CHARACTERISTICS PROVIDED :${characteristicsUuid}" }
            return null
        }
        val isEnabled = _cccDescriptorMap[deviceAddress] ?: false
        // TODO: INDICATION IS MARKED AS FALSE
        val isIndication = false
        return when (isEnabled) {
            true if (isIndication) -> BLEConstants.BLE_DESCRIPTOR_ENABLE_INDICATION
            true -> BLEConstants.BLE_DESCRIPTOR_ENABLE_NOTIFICATION
            false -> BLEConstants.BLE_DESCRIPTOR_DISABLE_NOTIFICATION
        }
    }

    override fun onWriteDescriptor(
        deviceAddress: String?,
        serviceUuid: String?,
        characteristicsUuid: String?,
        descriptorUuid: String?,
        value: ByteArray?
    ) {
        if (deviceAddress == null || descriptorUuid == null || value == null || characteristicsUuid == null) return

        Logger.i(TAG) { "WRITE REQUEST DESCRIPTOR ID $descriptorUuid CHARACTERISTIC ID : $characteristicsUuid" }

        val parsedDescriptorUUID = Uuid.parse(descriptorUuid)
        val parsedCharacteristicsUUID = Uuid.parse(characteristicsUuid)

        if (parsedDescriptorUUID != BLEConstants.CCC_DESCRIPTOR) {
            Logger.d(TAG) { "INVALID DESCRIPTOR PROVIDED ONLY CCC DESCRIPTOR ALLOWED" }
            return
        }
        if (parsedCharacteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) {
            Logger.d(TAG) { "INVALID CHARACTERISTICS PROVIDED :${characteristicsUuid}" }
            return
        }
        val isNotifyEnabled =
            value.contentEquals(BLEConstants.BLE_DESCRIPTOR_ENABLE_NOTIFICATION) ||
                    value.contentEquals(BLEConstants.BLE_DESCRIPTOR_ENABLE_INDICATION)

        if (parsedCharacteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) {
            Logger.d(TAG) { "INVALID CHARACTERISTICS PROVIDED :$characteristicsUuid" }
            return
        }
        _cccDescriptorMap[deviceAddress] = isNotifyEnabled
    }

    fun setRunning(value: Boolean) {
        _isRunning.value = value
    }

    fun cleanUp() {
        if (_scope.isActive) _scope.cancel()
        // clear the state-flows
        _incomingDeviceData.update { emptyList() }
        // clear the maps
        _localNonceMap.clear()
        _cccDescriptorMap.clear()
    }

}