package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.blejavaadvertise.callbacks.GATTServerCallback
import com.sam.blejavaadvertise.models.GATTBluetoothError
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.ble.models.BLESyncData
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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
    localDeviceInfo: LocalDeviceInfoProvider,
    private val protoBuf: ProtoBuf,
    private val randomGenerator: RandomGenerator,
    private val platformInfoProvider: PlatformInfoProvider,
) : GATTServerCallback {

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _deviceNonceMap = ConcurrentHashMap<String, ByteArray>()
    private val _bleCCCDescriptorMap = ConcurrentHashMap<String, Boolean>()

    private val _deviceInfo = localDeviceInfo.readDeviceInfo
        .stateIn(_scope, SharingStarted.Eagerly, null)

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _incomingPeerData = MutableStateFlow<List<BLEPeerData>>(emptyList())
    val externalPeers = _incomingPeerData.asStateFlow()

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

            Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicsUUID FROM DISCOVERY SERVICE" }

            val deviceInfo = _deviceInfo.value ?: return null

            val value = try {
                val nonce = randomGenerator.generateRandomBytes(NONCE_SIZE).also { nonceBytes ->
                    _deviceNonceMap[deviceAddress] = nonceBytes
                }
                val peerData = BLEPeerData(
                    deviceId = deviceInfo.deviceId,
                    deviceName = deviceInfo.name,
                    nonce = nonce.decodeToString(),
                    deviceOs = platformInfoProvider.platformOS,
                )
                protoBuf.encodeToByteArray<BLEPeerData>(peerData)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                null
            }

            Logger.d(TAG) { "SENDING SUCCESS RESPONSE FOR CHARACTERISTICS :${characteristicUuid}" }
            return value
        }
        // handle the sync service here
        if (serviceUUID == BLEConstants.SYNC_SERVICE_ID) {
            if (characteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) return null

            Logger.d(TAG) { "READ REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM SYNC SERVICE" }
            val deviceInfo = _deviceInfo.value ?: return null

            val value = try {
                val nonce = randomGenerator.generateRandomBytes(NONCE_SIZE).also { nonceBytes ->
                    _deviceNonceMap[deviceAddress] = nonceBytes
                }
                val peerData = BLESyncData.BLEAdvertiseData(
                    deviceId = deviceInfo.deviceId,
                    nonce = nonce.decodeToString(),
                    allowSync = true
                )
                val bytes = protoBuf.encodeToByteArray<BLESyncData.BLEAdvertiseData>(peerData)
                Logger.d(TAG) { "SENDING DATA SIZE:${bytes.size}" }
                bytes
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                null
            }
            Logger.d(TAG) { "SENDING SUCCESS RESPONSE FOR CHARACTERISTICS :${characteristicUuid}" }
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
        Logger.d(TAG) { "WRITE REQUESTED ON :$characteristicsUUID SERVICE:$serviceUUID" }

        if (serviceUUID == BLEConstants.DEVICE_INFO_SERVICE_ID) {
            if (characteristicsUUID != BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID) return
            Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicsUUID FROM DISCOVERY SERVICE" }
            val peerData = try {
                protoBuf.decodeFromByteArray<BLEPeerData>(value)
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                return
            }
            _incomingPeerData.update { devices -> (devices + peerData).distinctBy { it.deviceId } }
        }

        // HANDLE SYNC SERVICE ID
        if (serviceUUID == BLEConstants.SYNC_SERVICE_ID) {
            if (characteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) return
            Logger.d(TAG) { "WRITE REQUEST WITH CHARACTERISTIC : $characteristicUuid FROM SYNC SERVICE" }
            try {
                val response = protoBuf.decodeFromByteArray<BLESyncData.BLEAdvertiseResponse>(value)
                Logger.d(TAG) { "RESPONSE FOUND :$response" }
                // then sync ackno
                val acknowledgment = BLESyncData.BLESyncAcknowledgement(
                    nonce = response.nonce,
                    serverID = Uuid.random()
                )
                val ackBytes =
                    protoBuf.encodeToByteArray<BLESyncData.BLESyncAcknowledgement>(acknowledgment)
                _notifyCharacteristicsChanged?.invoke(
                    deviceAddress,
                    characteristicUuid,
                    false,
                    ackBytes
                )
            } catch (e: Exception) {
                Logger.w(TAG, e) { "UNABLE TO SERIALIZE THE DATA" }
                return
            }
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
        val isEnabled = _bleCCCDescriptorMap[deviceAddress] ?: false
        // TODO: INDICATION IS MARKED AS FALSE
        val isIndication = false
        return when (isEnabled) {
            true if (isIndication) -> byteArrayOf(0x02, 0x00)
            true -> byteArrayOf(0x01, 0x00)
            false -> byteArrayOf(0x00, 0x00)
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
        val isNotifyEnabled = when {
            value.contentEquals(byteArrayOf(0x01, 0x00)) -> true
            value.contentEquals(byteArrayOf(0x02, 0x00)) -> true
            value.contentEquals(byteArrayOf(0x00, 0x00)) -> false
            else -> return
        }

        if (parsedCharacteristicsUUID != BLEConstants.SYNC_CHARACTERISTICS_ID) {
            Logger.d(TAG) { "INVALID CHARACTERISTICS PROVIDED :$characteristicsUuid" }
            return
        }
        _bleCCCDescriptorMap[deviceAddress] = isNotifyEnabled
    }

    fun setRunning(value: Boolean) {
        _isRunning.value = value
    }

    fun cleanUp() {
        if (_scope.isActive) _scope.cancel()
        _incomingPeerData.update { emptyList() }
        _deviceNonceMap.clear()
        _bleCCCDescriptorMap.clear()
    }

}