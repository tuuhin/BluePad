package com.sam.bluepad.data.ble.delegate

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.BLEHandshakeFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.exceptions.InvalidCCCDescriptorException
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.use_cases.BytesEncoder
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.Uuid

class BLEAdvertiserSyncHandlerDelegate(
    val protoBuf: ProtoBuf,
    val randomGenerator: RandomGenerator,
    val platformInfoProvider: PlatformInfoProvider,
    val encoder: BytesEncoder,
    val inPayloadManager: InPayloadManager,
    val outPayloadManager: OutPayloadManager,
) {

    val lock = Mutex()
    val localNonceMap = HashMap<String, String>()
    val cccDescriptorMap = HashMap<String, Boolean>()

    fun handleDeviceReadRequest(currentDeviceInfo: LocalDeviceInfoModel? = null): Result<ByteArray> {
        return runCatching {
            if (currentDeviceInfo == null) throw LocalIdentityMissingException()

            val nonce = randomGenerator.generateRandomBytes(size = NONCE_SIZE)
            val peerData = BLEPeerData(
                deviceId = currentDeviceInfo.deviceId,
                deviceOs = platformInfoProvider.platformOS,
                deviceName = currentDeviceInfo.name,
                nonce = nonce.decodeToString(),
            )
            protoBuf.encodeToByteArray<BLEPeerData>(peerData)
        }.onFailure { err -> Logger.e(TAG, err) { "CANNOT HANDLE DEVICE READ REQUEST" } }
    }

    fun handleDeviceWriteRequest(value: ByteArray): Result<BLEPeerData> {
        return runCatching {
            protoBuf.decodeFromByteArray<BLEPeerData>(value)
        }.onFailure { err -> Logger.e(TAG, err) { "CANNOT HANDLE DEVICE WRITE REQUEST" } }
    }

    suspend fun handleProximityReadRequest(
        address: String,
        currentDevice: LocalDeviceInfoModel? = null
    ): Result<ByteArray> {
        return runCatching {
            if (currentDevice == null) throw LocalIdentityMissingException()

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
            protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseDeviceData>(data)
        }.onFailure { err -> Logger.e(TAG, err) { "CANNOT HANDLE PROXIMITY READ REQUEST" } }
    }

    suspend inline fun handleProximityWriteRequest(
        address: String,
        value: ByteArray,
        currentDeviceInfo: LocalDeviceInfoModel? = null,
        savedDevices: suspend (Uuid) -> Result<ExternalDeviceModel>,
        onNotify: suspend (ByteArray) -> Unit,
    ): Result<ExternalDeviceModel> {
        return runCatching {

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

            val bytes = protoBuf.encodeToByteArray<BLESyncHandshakeData>(data)
            onNotify(bytes)

            val message = when (data) {
                is BLESyncHandshakeData.HandshakeACKSuccess -> "ACK SUCCESS"
                is BLESyncHandshakeData.HandshakeACKFailed -> "ACK FAILED : REASON :${data.reason}"
                else -> ""
            }
            Logger.d(TAG) { "SENDING ACK DATA ACK RESULT: $message" }
            if (externalDevice == null) throw UnrecognizedPeerDeviceException(response.senderID)
            externalDevice
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Logger.e(TAG, err) { "FAILED TO HANDLE PROXIMITY WRITE REQUEST" }
        }
    }

    suspend inline fun handleSyncDataWriteRequest(
        value: ByteArray,
        onNotify: suspend (ByteArray) -> Boolean,
    ) = runCatching {
        // read the response
        val opResult = when (val response = protoBuf.decodeFromByteArray<BLESyncSession>(value)) {
            BLESyncSession.SyncSessionStart -> sendSessionStartACK(onNotify)
            is BLESyncSession.BLESyncDataPacket -> manageSyncSessionDataPacket(response, onNotify)
            is BLESyncSession.BLESyncDataAck -> manageSyncSessionDataPacketAck(response, onNotify)
            is BLESyncSession.BLESyncDataPacketEnd -> markSyncSessionPacketEnded(response, onNotify)
            is BLESyncSession.SyncPacketTransition -> checkTransitionAckAndSendDataPacket(response, onNotify)
            BLESyncSession.SyncPacketProcessing -> runCatching {
                Logger.d(TAG) { "PROCESSING" }
                true
            }

            is BLESyncSession.SyncSessionFailed -> runCatching {
                Logger.d(TAG) { "SYNC SESSION FAILED :${response.reason}" }
                true
            }

            BLESyncSession.SyncSessionSuccessful -> runCatching {
                Logger.d(TAG) { "SYNC SESSION SUCCESSFULLY" }
                true
            }

            else -> throw UnsupportedSyncSessionException(response)
        }
        opResult.getOrElse { err -> throw err }
        Unit
    }.onFailure { err ->
        if (err is CancellationException) throw err
        Logger.e(TAG, err) { "CANNOT HANDLE THIS OPERATION ${err.message}" }
    }

    suspend inline fun manageSyncSessionDataPacketAck(
        data: BLESyncSession.BLESyncDataAck,
        onNotify: suspend (ByteArray) -> Boolean,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED A DATA PACKET ACK TYPE:${data.type}" }
        return runCatching {
            // mark the payload as consumed
            outPayloadManager.markChunkAck(data.sequenceNumber)

            // check if wr have more bytes that can be sent
            if (!outPayloadManager.getHasMoreChunks()) {
                // send we are done with sending metadata packet
                val response = BLESyncSession.BLESyncDataPacketEnd(type = data.type)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                return@runCatching onNotify(bytes)
            }

            val chunk = outPayloadManager.getNextChunk()
                .getOrElse { err ->
                    Logger.w(TAG, err) { "ISSUE WITH NEXT CHUNK" }
                    // mark this as failed
                    val response = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.TAMPERED_DATA, true)
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                    return@runCatching onNotify(bytes)
                }

            // now send the response as the payload block
            val response = BLESyncSession.BLESyncDataPacket(data.type, chunk.seqNumber, chunk.payload)
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
            onNotify(bytes)
        }
    }

    suspend inline fun markSyncSessionPacketEnded(
        payload: BLESyncSession.BLESyncDataPacketEnd,
        onNotify: suspend (ByteArray) -> Boolean,
    ): Result<Boolean> = runCatching {
        Logger.d(TAG) { "SESSION PACKET END RECEIVED TYPE:${payload.type}" }

        // send sync packet processing
        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncPacketProcessing)
        onNotify(bytes)

        // process the payload
        val processedResult = inPayloadManager.processData()
        val data = when (val result = processedResult.getOrThrow()) {
            is SyncDataPayload.ContentIdsQuery if result.ids.isEmpty() -> {
                // DATA IS COMPLETELY SAME NO NEED TO SYNC ANYTHING
                BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.CONTENT_SAME, false)
            }

            is SyncDataPayload.ContentPayload if result.contentData.isEmpty() -> {
                // DATA IS COMPLETELY SAME NO NEED TO SYNC ANYTHING
                BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.CONTENT_SAME, false)
            }

            is SyncDataPayload.ContentIdsQuery if (payload.type == BLESyncDataType.METADATA) -> {
                // LOAD THE DATA AND TRANSITION FROM METADAT TO CONTENT_REQ
                outPayloadManager.prepareChunks(result)
                BLESyncSession.SyncPacketTransition(BLESyncDataType.METADATA, BLESyncDataType.CONTENT_REQUEST)
            }

            is SyncDataPayload.ContentPayload if payload.type == BLESyncDataType.CONTENT_REQUEST -> {
                // LOAD THE DATA AND TRANSITION FROM CONTENT REQ TO CONTENT
                outPayloadManager.prepareChunks(result)
                // now send a transition request
                BLESyncSession.SyncPacketTransition(BLESyncDataType.CONTENT_REQUEST, BLESyncDataType.CONTENT)
            }

            SyncDataPayload.SuccessAndNoAction -> {
                // Half duplex sync done
                val successBytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncSessionSuccessful)
                onNotify(successBytes)

                Logger.d(TAG) { "STARTING THE SECOND HALF-DUPLEX SYNC" }
                // NOW WE NEED TO SEND THE METADATA FROM THE ADVERTISER
                outPayloadManager.prepareChunks(SyncDataPayload.Metadata)
                // now send a transition request
                BLESyncSession.SyncPacketTransition(BLESyncDataType.CONTENT, BLESyncDataType.METADATA)
            }

            else -> throw InvalidSyncPayloadException()
        }
        Logger.d(TAG) { "SYNC PACKET TO BE SEND : $data" }
        val packetData = protoBuf.encodeToByteArray<BLESyncSession>(data)
        onNotify(packetData)
    }

    suspend inline fun checkTransitionAckAndSendDataPacket(
        payload: BLESyncSession.SyncPacketTransition,
        onNotify: suspend (ByteArray) -> Boolean,
    ) = runCatching {
        // if the transition requested send an ack
        when {
            payload.isRequested -> {
                // clear the buffer and send ack
                inPayloadManager.clearBuffer()
                outPayloadManager.reset()

                val dataAck = payload.copy(isRequested = false, isAck = true)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(dataAck)
                return@runCatching onNotify(bytes)
            }

            // transition need to be ack
            !payload.isAck -> {
                Logger.w(TAG) { "MISSING ACK FLAG OR NEW DATA TYPE IS NOT MENTIONED" }
                val session = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.MISSING_FLAG, true)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                return@runCatching onNotify(bytes)
            }

            !outPayloadManager.getHasMoreChunks() -> {
                // send we are done with sending metadata packet
                val response = BLESyncSession.BLESyncDataPacketEnd(type = payload.newType)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                return@runCatching onNotify(bytes)
            }

            // now send the response
            else -> {
                val chunkResult = outPayloadManager.getNextChunk()
                // we have a block
                val chunk = chunkResult.getOrElse { err ->
                    Logger.w(TAG, err) { "A CHUNK OF DATA SHOULD BE PRESENT" }
                    val session = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.INVALID_STATE, true)
                    val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                    return@runCatching onNotify(bytes)
                }

                // now send the response
                val response = BLESyncSession.BLESyncDataPacket(payload.newType, chunk.seqNumber, chunk.payload)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                onNotify(bytes)
            }
        }
    }


    suspend inline fun manageSyncSessionDataPacket(
        data: BLESyncSession.BLESyncDataPacket,
        onNotify: suspend (ByteArray) -> Boolean,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED A DATA PACKET TYPE:${data.type}" }

        return runCatching {
            // add the chunk to the payload
            inPayloadManager.addIncomingPayloadChunk(data.sequenceNumber, data.payload)
            val data = BLESyncSession.BLESyncDataAck(data.type, data.sequenceNumber)
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(data)
            onNotify(bytes)
        }
    }

    suspend inline fun sendSessionStartACK(onNotify: suspend (ByteArray) -> Boolean)
        : Result<Boolean> {
        // clear buffers for both cases
        outPayloadManager.reset()
        inPayloadManager.clearBuffer()

        Logger.d(TAG) { "SESSION START ACK" }
        val data = BLESyncSession.SyncSessionStartAck(true)
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(data)
            onNotify(bytes)
        }
    }

    fun markDeviceDisconnectedAndClearCache(address: String) {
        localNonceMap.remove(address)
        cccDescriptorMap.remove(address)
    }

    fun cleanUp() {
        localNonceMap.clear()
        cccDescriptorMap.clear()
    }

    suspend fun handleCCCWriteRequest(
        address: String,
        descriptorUuid: Uuid,
        value: ByteArray,
    ): Result<Unit> = runCatching {
        if (descriptorUuid != BLEConstants.CCC_DESCRIPTOR)
            throw InvalidCCCDescriptorException()

        lock.withLock {
            cccDescriptorMap[address] = value.btDescriptorsNotificationOrIndicationEnabled
        }
        val bytesAsString = value.joinToString("-") { it.toHexString() }
        Logger.d(TAG) { "UPDATED DESCRIPTOR VALUE :$bytesAsString" }
    }

    suspend fun handleCCCReadRequest(
        address: String,
        isIndication: Boolean = true,
        descriptorUuid: Uuid,
    ): Result<ByteArray> = runCatching {

        if (descriptorUuid != BLEConstants.CCC_DESCRIPTOR)
            throw InvalidCCCDescriptorException()

        val isEnabled = lock.withLock { cccDescriptorMap[address] ?: false }
        val bytes = isEnabled.asCCCDescriptorValue(isIndication)
        val bytesAsString = bytes.joinToString("-") { it.toHexString() }
        Logger.d(TAG) { "DESCRIPTOR READ VALUE : $bytesAsString" }
        bytes
    }


    class UnsupportedSyncSessionException(session: BLESyncSession) :
        Exception("The sync session request of type ${session::class.simpleName} is not supported or was received out of sequence")

    class LocalIdentityMissingException :
        Exception("Local device information is required for BLE identification but was not provided.")

    class UnrecognizedPeerDeviceException(val deviceId: Uuid) :
        Exception("Sync denied: The device $deviceId is not in the list of authorized or saved devices.")

    class InvalidSyncPayloadException :
        Exception("The synchronization data was processed successfully, but the resulting payload state is invalid for current transaction")

    companion object {
        const val NONCE_SIZE = 16
        const val TAG = "SERVER_METHOD_HANDLER"
    }
}
