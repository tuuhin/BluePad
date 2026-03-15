package com.sam.bluepad.data.ble.delegate

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.BLEHandshakeFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncHandshakeData
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.exceptions.EmptyPayloadException
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.Uuid

class BLEConnectorSyncHandlerDelegate(
    val protoBuf: ProtoBuf,
    val outPayloadManager: OutPayloadManager,
    val inPayloadManager: InPayloadManager,
) {

    val lock = Mutex()
    val handShakeDataMap = HashMap<String, BLESyncHandshakeData.AdvertiseResponseData>()
    val hadShakeNotificationMap = ConcurrentHashMap<String, Boolean>()


    suspend inline fun handleHandshakeRead(
        deviceAddress: String,
        value: ByteArray,
        deviceInfo: LocalDeviceInfoModel?,
        savedDevices: suspend (Uuid) -> Result<ExternalDeviceModel>,
        onReadSuccess: () -> Boolean,
    ): Result<ExternalDeviceModel> = runCatching {

        val currentDeviceInfo = deviceInfo ?: throw LocalDeviceInfoMissing()
        val syncData = protoBuf.decodeFromByteArray<BLESyncHandshakeData.AdvertiseDeviceData>(value)

        if (!syncData.allowSync) {
            Logger.e(TAG) { "SYNC FLAG MISSING" }
            throw SyncFlagMissingException()
        }

        val externalDevice = savedDevices(syncData.deviceId).getOrElse { err ->
            Logger.w(TAG) { "CANNOT FIND THE GIVEN DEVICE " }
            return Result.failure(err)
        }

        Logger.d(TAG) { "ADVERTISE DATA RECEIVED DEVICE_ID:${syncData.deviceId} VERIFIED" }
        // on write notification fully active we will send the outgoing data
        onReadSuccess()

        val outgoingData = BLESyncHandshakeData.AdvertiseResponseData(
            nonce = syncData.nonce,
            receiverID = syncData.deviceId,
            senderID = currentDeviceInfo.deviceId,
        )

        lock.withLock {
            // saving the content data on the cache map
            handShakeDataMap[deviceAddress] = outgoingData
            hadShakeNotificationMap[deviceAddress] = true
        }
        externalDevice

    }

    suspend inline fun handleHandshakeNotification(
        value: ByteArray,
        onHandshakeSuccess: suspend () -> Boolean
    ): Result<ConnectorSyncEvent> {
        return runCatching {
            val result = protoBuf.decodeFromByteArray<BLESyncHandshakeData>(value)
            Logger.i(TAG) { "HANDSHAKE ACK DATA FOUND" }
            // handle the result
            when (result) {
                is BLESyncHandshakeData.HandshakeACKFailed -> {
                    val error = InvalidAcknowledgementException(result.reason)
                    Logger.d(TAG, error) { "FAILED ACKNOWLEDGEMENT FOUND REASON:${result.reason}" }
                    throw InvalidAcknowledgementException(result.reason)
                }

                is BLESyncHandshakeData.HandshakeACKSuccess -> {
                    Logger.i(TAG) { "HANDSHAKE SUCCESSFUL TURING OFF ADVERTISEMENTS" }
                    // send this after some time to clear the bluetooth stack
                    onHandshakeSuccess()
                }

                else -> throw InvalidHandshakeValueException()
            }
            ConnectorSyncEvent.AdvertisingAcknowledgmentReceived
        }
    }

    suspend inline fun handleSyncDataNotification(
        characteristicId: Uuid,
        value: ByteArray,
        onEvent: (ConnectorSyncEvent) -> Unit,
        onWriteBytes: suspend (ByteArray) -> Boolean,
        onToggleNotification: suspend (characteristics: Uuid, enable: Boolean) -> Unit,
        onError: (Throwable?) -> Unit,
    ) {
        try {
            val decodedData = protoBuf.decodeFromByteArray<BLESyncSession>(value)
            Logger.d(TAG) { "SYNC DATA RECEIVED  | BLOCK_SIZE: ${value.size}" }

            val result = when (decodedData) {
                is BLESyncSession.SyncSessionStartAck -> onSessionStartACK(decodedData, onWriteBytes)
                is BLESyncSession.BLESyncDataPacket -> onDataPacketReceived(decodedData, onWriteBytes)
                is BLESyncSession.BLESyncDataAck -> onDataPacketACKReceived(decodedData, onWriteBytes)
                is BLESyncSession.BLESyncDataPacketEnd -> onDataPacketEnd(decodedData, onWriteBytes)
                is BLESyncSession.SyncPacketTransition -> onPacketTransition(decodedData, onWriteBytes)
                BLESyncSession.SyncSessionSuccessful -> runCatching {
                    Logger.d(TAG) { "SYNC SESSION COMPLETED" }
                }

                is BLESyncSession.SyncSessionFailed -> runCatching {
                    Logger.d(TAG) { "SYNC SESSION FAILED" }
                    onToggleNotification(characteristicId, false)
                }

                BLESyncSession.SyncPacketProcessing -> {
                    Logger.d(TAG) { "REMOTE PROCESSING DATA RUNNING...." }
                    onEvent(ConnectorSyncEvent.RemoteProcessing)
                    Result.success(true)
                }

                else -> Result.failure(InvalidSessionTypeException())
            }

            if (result.isFailure) {
                onError(result.exceptionOrNull())
                return
            }
        } catch (_: SerializationException) {
            Logger.e(TAG) { "INVALID DATA RECEIVED CANNOT DECODE IT" }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(TAG, e) { "UNKNOWN EXCEPTION" }
        }
    }


    suspend inline fun onDataPacketEnd(
        data: BLESyncSession.BLESyncDataPacketEnd,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ) = runCatching {

        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncPacketProcessing)
        onWriteBytes(bytes)

        Logger.d(TAG) { "PACKET END MARKER RECEIVED TYPE:${data.type}" }
        val result = inPayloadManager.processData().getOrThrow()

        Logger.d(TAG) { "PROCESSED RESULT:$result" }
        if (result is SyncDataPayload.Outgoing) outPayloadManager.prepareChunks(result)

        // handle the result
        val transitionPacket = when (result) {
            is SyncDataPayload.ContentPayload if (data.type == BLESyncDataType.CONTENT_REQUEST) ->
                BLESyncSession.SyncPacketTransition(BLESyncDataType.CONTENT_REQUEST, BLESyncDataType.CONTENT)

            is SyncDataPayload.ContentIdsQuery if (data.type == BLESyncDataType.METADATA) ->
                BLESyncSession.SyncPacketTransition(BLESyncDataType.METADATA, BLESyncDataType.CONTENT_REQUEST)

            is SyncDataPayload.SuccessAndNoAction -> {
                val packetBytes = protoBuf.encodeToByteArray<BLESyncSession>(BLESyncSession.SyncSessionSuccessful)
                return@runCatching onWriteBytes(packetBytes)
            }

            else -> throw InvalidPayloadDataException()
        }

        val packetBytes = protoBuf.encodeToByteArray<BLESyncSession>(transitionPacket)
        onWriteBytes(packetBytes)
    }


    suspend inline fun onPacketTransition(
        data: BLESyncSession.SyncPacketTransition,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<Boolean> {
        Logger.d(TAG) { "PACKET TYPE TRANSITION TO ${data.newType} FROM :${data.prevType}" }

        return runCatching {
            val responseData = when {
                // if the is request send ack
                data.isRequested -> {
                    // clear the buffer and send ack
                    inPayloadManager.clearBuffer()
                    outPayloadManager.reset()
                    Logger.w(TAG) { "REQUESTED TRANSITION REQUEST RESPONDING WITH ACK" }
                    data.copy(isRequested = false, isAck = true)
                }

                // if no ack flag found
                !data.isAck -> {
                    Logger.w(TAG) { "MISSING ACK FLAG STOPPING SYNC SESSION" }
                    BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.MISSING_FLAG, true)
                }

                // if no chunk data present
                !outPayloadManager.getHasMoreChunks() -> {
                    // send we are done with sending metadata packet
                    Logger.w(TAG) { "NO MORE CHUNKS TO WORK WITH SESSION DATA PACKET END" }
                    BLESyncSession.BLESyncDataPacketEnd(type = data.newType)
                }

                else -> {
                    // now send the response
                    val chunkResult = outPayloadManager.getNextChunk()
                    val chunk = chunkResult.getOrElse { err ->
                        Logger.w(TAG, err) { "A CHUNK OF DATA SHOULD BE PRESENT" }
                        val session = BLESyncSession.SyncSessionFailed(reason = BLESyncFailedReason.INVALID_STATE, true)
                        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                        return@runCatching onWriteBytes(bytes)
                    }

                    // now send the response
                    BLESyncSession.BLESyncDataPacket(BLESyncDataType.CONTENT_REQUEST, chunk.seqNumber, chunk.payload)
                }
            }
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(responseData)
            onWriteBytes(bytes)
        }
    }

    suspend inline fun onDataPacketReceived(
        data: BLESyncSession.BLESyncDataPacket,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED PACKET DATA FROM OTHER DEVICE TYPE:${data.type}" }

        return runCatching {
            inPayloadManager.addIncomingPayloadChunk(data.sequenceNumber, data.payload)
            val data = BLESyncSession.BLESyncDataAck(data.type, data.sequenceNumber)
            val sessionData = protoBuf.encodeToByteArray<BLESyncSession>(data)
            onWriteBytes(sessionData)
        }
    }

    suspend inline fun onDataPacketACKReceived(
        data: BLESyncSession.BLESyncDataAck,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<Boolean> {
        Logger.d(TAG) { "RECEIVED PACKET ACK DATA FROM OTHER DEVICE" }
        // mark the payload as consumed
        outPayloadManager.markChunkAck(data.sequenceNumber)

        if (!outPayloadManager.getHasMoreChunks()) {
            // send we are done with sending metadata packet
            return runCatching {
                val response = BLESyncSession.BLESyncDataPacketEnd(type = data.type)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                onWriteBytes(bytes)
            }
        }
        val chunkResult = outPayloadManager.getNextChunk()
        // we have a block
        val chunk = chunkResult.getOrElse { err ->
            Logger.w(TAG, err) { "ISSUE WITH NEXT CHUNK" }
            return Result.failure(err)
        }
        val response = BLESyncSession.BLESyncDataPacket(
            type = data.type,
            sequenceNumber = chunk.seqNumber,
            payload = chunk.payload,
        )
        // now send the response
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
            onWriteBytes(bytes)
        }
    }

    suspend inline fun onSessionStartACK(
        response: BLESyncSession.SyncSessionStartAck,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<Boolean> {
        // response ack flag should be true
        if (!response.isAck) return Result.failure(SyncStarkNotAckException())

        outPayloadManager.prepareChunks(SyncDataPayload.Metadata).getOrElse { err ->
            Logger.w(TAG) { "CANNOT PREPARE THE BLOCKS" }
            return Result.failure(err)
        }

        // chunks should be probably ready by now
        if (!outPayloadManager.getHasMoreChunks()) return Result.failure(EmptyPayloadException())
        val chunk = outPayloadManager.getNextChunk()
            .getOrElse { err ->
                Logger.w(TAG, err) { "ISSUE WITH NEXT CHUNK" }
                return Result.failure(err)
            }
        // we have a block
        val response = BLESyncSession.BLESyncDataPacket(BLESyncDataType.METADATA, chunk.seqNumber, chunk.payload)
        Logger.d(TAG) { "SENDING FIRST BLOCK OF METADATA CHUNKS" }
        // now send the response
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
            onWriteBytes(bytes)
        }
    }

    suspend inline fun onEnabledDisabledCCCDescriptor(
        address: String,
        characteristicId: Uuid,
        bytes: ByteArray,
        onWriteBytes: (ByteArray) -> Boolean,
        onToggleNotification: (characteristics: Uuid, enable: Boolean) -> Unit,
    ) {
        val isEnabled = bytes.isCCCDescriptorEnabled

        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if isEnabled -> {
                // thus notification is turned on successfully
                val outgoingData = lock.withLock { handShakeDataMap[address] } ?: return
                val syncWrite = protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseResponseData>(outgoingData)
                val response = onWriteBytes(syncWrite)
                Logger.d(TAG) { "WRITING ADVERTISING RESPONSE CHARACTERISTICS IS_SUCCESS:$response" }
            }

            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID -> lock.withLock {
                val isNotificationOn = hadShakeNotificationMap[address] ?: false
                if (!isNotificationOn) return
                hadShakeNotificationMap.remove(address)
                Logger.d(TAG) { "TURNING OF HANDSHAKE NOTIFICATION AND TURNING ON DATA NOTIFICATION" }

                onToggleNotification(BLEConstants.SYNC_DATA_CHARACTERISTICS_ID, true)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if isEnabled -> {
                Logger.d(TAG) { "STARTING CHARACTERISTICS NOTIFICATION TURNED ON" }
                val sendData = BLESyncSession.SyncSessionStart
                val bytesToSend = protoBuf.encodeToByteArray<BLESyncSession>(sendData)
                onWriteBytes(bytesToSend)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> Logger.d(TAG) { "SYNCING IS DONE NOW SYNC NOTIFICATION ARE DISMISSED" }
            else -> {
                val text = if (isEnabled) "ENABLED" else "DISABLED"
                Logger.d(TAG) { "GATT NOTIFICATION $text  FOR CHARACTERISTICS :${characteristicId}" }
            }
        }

    }

    // exceptions internal
    class InvalidSessionTypeException : Exception("Provided session type is invalid or any handler is not present")
    class SyncStarkNotAckException : Exception("Start is not ack properly missing ack flag")
    class InvalidHandshakeValueException : Exception("Invalid Handshake value")
    class InvalidAcknowledgementException(reason: BLEHandshakeFailedReason) :
        Exception("Invalid Acknowledgement :${reason.name}")

    class InvalidPayloadDataException : Exception("Invalid payload type its not supported")

    class SyncFlagMissingException : Exception("No sync flag found in the read response")
    class LocalDeviceInfoMissing : Exception("Local device data need to be known")

    companion object {
        const val TAG = "SYNC_DEVICE_CONNECTION_DELEGATE"
    }
}
