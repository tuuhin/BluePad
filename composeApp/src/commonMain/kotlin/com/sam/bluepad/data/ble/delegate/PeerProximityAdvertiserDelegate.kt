package com.sam.bluepad.data.ble.delegate

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.exceptions.BLEAdvertiserException
import com.sam.bluepad.data.sync.dto.BLEHandshakeFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.exceptions.InvalidCCCDescriptorException
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.Uuid

class PeerProximityAdvertiserDelegate(
    private val protoBuf: ProtoBuf,
    private val randomGenerator: RandomGenerator,
    private val encoder: BytesEncoder,
    private val platformDispatcherProvider: PlatformDispatcherProvider,
) {

    val lock = Mutex()
    val localNonceMap = HashMap<String, String>()
    val cccDescriptorMap = HashMap<String, Boolean>()

    suspend fun handleProximityReadRequest(
        address: String,
        currentDevice: LocalDeviceInfoModel? = null
    ): Result<ByteArray> = runCatching {
        if (currentDevice == null) throw BLEAdvertiserException.LocalIdentityMissingException()

        val nonce = lock.withLock {
            randomGenerator.generateRandomBytes(NONCE_SIZE).let { nonceBytes ->
                encoder.encodeBytes(nonceBytes).apply { localNonceMap[address] = this }
            }
        }
        val data = BLESyncHandshakeData.AdvertiseDeviceData(
            deviceId = currentDevice.deviceId,
            nonce = nonce,
            allowSync = true,
        )
        Logger.d(tag = TAG) { "OUTGOING HANDSHAKE DATA" }
        withContext(platformDispatcherProvider.io) {
            protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseDeviceData>(data)
        }
    }.onFailure { err -> Logger.e(tag = TAG, throwable = err) { "CANNOT HANDLE PROXIMITY READ REQUEST" } }


    suspend fun handleProximityWriteRequest(
        address: String,
        value: ByteArray,
        currentDeviceInfo: LocalDeviceInfoModel? = null,
        savedDevices: suspend (Uuid) -> Result<ExternalDeviceModel>,
        onNotify: suspend (ByteArray) -> Unit,
    ): Result<ExternalDeviceModel> = runCatching {

        val response = protoBuf.decodeFromByteArray<BLESyncHandshakeData.AdvertiseResponseData>(value)

        val savedNonce = lock.withLock { localNonceMap[address] }
        val externalDevice = savedDevices(response.senderID).getOrNull()

        val failedReason = when {
            savedNonce == null -> BLEHandshakeFailedReason.INVALID_INCOMING_DATA
            savedNonce != response.nonce -> BLEHandshakeFailedReason.TAMPERED_DATA
            response.receiverID != currentDeviceInfo?.deviceId -> BLEHandshakeFailedReason.INVALID_RECEIVER
            externalDevice == null -> BLEHandshakeFailedReason.UNKNOWN_SENDER
            else -> null
        }

        val data = failedReason?.let { BLESyncHandshakeData.HandshakeACKFailed(it) }
            ?: BLESyncHandshakeData.HandshakeACKSuccess(response.nonce)

        val bytes = withContext(platformDispatcherProvider.io) {
            protoBuf.encodeToByteArray<BLESyncHandshakeData>(data)
        }
        onNotify(bytes)

        val message = when (data) {
            is BLESyncHandshakeData.HandshakeACKSuccess -> "ACK SUCCESS"
            is BLESyncHandshakeData.HandshakeACKFailed -> "ACK FAILED : REASON :${data.reason}"
            else -> ""
        }
        Logger.d(tag = TAG) { "SENDING ACK DATA ACK RESULT: $message" }
        if (externalDevice == null) throw BLEAdvertiserException.UnrecognizedPeerDeviceException(response.senderID)
        Logger.d(tag = TAG) { "INCOMING HANDSHAKE DEVICE :$externalDevice" }
        externalDevice
    }.onFailure { err ->
        if (err is CancellationException) throw err
        Logger.e(tag = TAG) { "FAILED TO HANDLE PROXIMITY WRITE REQUEST" }
    }

    suspend fun handleCCCWriteRequest(
        address: String,
        descriptorUuid: Uuid,
        characteristicsId: Uuid,
        value: ByteArray,
    ): Result<Unit> = runCatching {
        if (descriptorUuid != BLEConstants.CCC_DESCRIPTOR) throw InvalidCCCDescriptorException()
        // lock to prevent it from modifying concurrently
        lock.withLock {
            cccDescriptorMap[address] = value.btDescriptorsNotificationOrIndicationEnabled
        }
        Logger.d(tag = TAG) { "DESCRIPTOR WRITE REQUESTED BY $address CHARACTERISTICS:$characteristicsId VALUE:0x${value.toHexString()}" }
    }

    suspend fun handleCCCReadRequest(
        address: String,
        descriptorUuid: Uuid,
        characteristicsId: Uuid,
        isIndication: Boolean = true,
    ): Result<ByteArray> = runCatching {

        if (descriptorUuid != BLEConstants.CCC_DESCRIPTOR) throw InvalidCCCDescriptorException()
        // update the value with lock
        val isEnabled = lock.withLock { cccDescriptorMap[address] ?: false }
        val bytes = isEnabled.asCCCDescriptorValue(isIndication)

        Logger.d(tag = TAG) { "DESCRIPTOR READ REQUESTED BY $address CHARACTERISTICS:$characteristicsId VALUE :0x${bytes.toHexString()}" }
        bytes
    }


    fun markDisconnected(address: String) {
        Logger.d(tag = TAG) { "ADDRESS :$address MARKED DISCONNECTED" }
        localNonceMap.remove(address)
        cccDescriptorMap.remove(address)
    }

    fun cleanUp() {
        Logger.d(tag = TAG) { "CLEARING NONCE MAP" }
        localNonceMap.clear()
        cccDescriptorMap.clear()
    }

    companion object {
        const val TAG = "PEER_PROXIMITY_ADVERTISER_DELEGATE"
        const val NONCE_SIZE = 12
    }
}
