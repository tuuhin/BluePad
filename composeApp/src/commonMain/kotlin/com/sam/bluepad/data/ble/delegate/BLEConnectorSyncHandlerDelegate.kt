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
import kotlin.uuid.Uuid

private typealias SessionResult = Pair<BLESyncSession?, Boolean>

class BLEConnectorSyncHandlerDelegate(
    val protoBuf: ProtoBuf,
    val outPayloadManager: OutPayloadManager,
    val inPayloadManager: InPayloadManager,
) {

    val lock = Mutex()
    val handShakeDataMap = HashMap<String, BLESyncHandshakeData.AdvertiseResponseData>()
    val hadShakeNotificationMap = HashMap<String, Boolean>()

    var currentSessionId: Uuid? = null

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
            Logger.e(tag = TAG) { "SYNC FLAG MISSING" }
            throw SyncFlagMissingException()
        }

        val externalDevice = savedDevices(syncData.deviceId).getOrElse { err ->
            Logger.w(tag = TAG) { "CANNOT FIND THE GIVEN DEVICE " }
            return Result.failure(err)
        }

        Logger.d(tag = TAG) { "ADVERTISE DATA RECEIVED DEVICE_ID:${syncData.deviceId} VERIFIED" }
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
    ): Result<Unit> {
        return runCatching {
            val result = protoBuf.decodeFromByteArray<BLESyncHandshakeData>(value)
            Logger.i(tag = TAG) { "HANDSHAKE ACK DATA FOUND" }
            // handle the result
            when (result) {
                is BLESyncHandshakeData.HandshakeACKFailed -> {
                    val error = InvalidAcknowledgementException(result.reason)
                    Logger.w(tag = TAG, throwable = error) { "FAILED ACKNOWLEDGEMENT FOUND REASON:${result.reason}" }
                    throw InvalidAcknowledgementException(result.reason)
                }

                is BLESyncHandshakeData.HandshakeACKSuccess -> {
                    Logger.i(tag = TAG) { "HANDSHAKE SUCCESSFUL TURING OFF ADVERTISEMENTS" }
                    // send this after some time to clear the bluetooth stack
                    onHandshakeSuccess()
                }

                else -> throw InvalidHandshakeValueException()
            }
        }
    }

    suspend inline fun handleSyncDataNotification(
        characteristicId: Uuid,
        value: ByteArray,
        onEvent: (ConnectorSyncEvent) -> Unit,
        onWriteBytes: suspend (ByteArray) -> Boolean,
        onReadDevice: () -> ExternalDeviceModel?,
        onToggleNotification: suspend (characteristics: Uuid, enable: Boolean) -> Boolean,
    ): Result<Boolean> {
        return try {
            val decodedData = protoBuf.decodeFromByteArray<BLESyncSession>(value)
            Logger.d(tag = TAG) { "SYNC DATA RECEIVED TYPE: ${decodedData::class.simpleName} | BLOCK_SIZE:${value.size}" }

            val device = onReadDevice()

            val eventIn = when (decodedData) {
                is BLESyncSession.SyncSessionStartAck if (device != null) -> ConnectorSyncEvent.SyncStarted(device)
                is BLESyncSession.SyncSessionFailed -> ConnectorSyncEvent.SyncFailed(decodedData.reason.name)
                else -> null
            }

            if (eventIn != null) onEvent(eventIn)

            val result = when (decodedData) {
                is BLESyncSession.SyncSessionStartAck -> onSessionStartACK(decodedData, onWriteBytes)
                is BLESyncSession.BLESyncDataPacket -> onDataPacketReceived(decodedData, onWriteBytes)
                is BLESyncSession.BLESyncDataAck -> onDataPacketACKReceived(decodedData, onWriteBytes)
                is BLESyncSession.BLESyncDataPacketEnd -> onDataPacketEnd(decodedData, onWriteBytes)
                is BLESyncSession.SyncPacketTransition -> onPacketTransition(decodedData, onWriteBytes)
                is BLESyncSession.SyncSessionSuccessfulAck -> runCatching {
                    Logger.d(tag = TAG) { "SYNC SESSION COMPLETED AND ACK" }
                    decodedData to true
                }

                is BLESyncSession.SyncSessionFailed -> runCatching {
                    Logger.d(tag = TAG) { "SYNC SESSION FAILED" }
                    decodedData to onToggleNotification(characteristicId, false)
                }

                is BLESyncSession.SyncPacketProcessing -> runCatching {
                    Logger.d(tag = TAG) { "REMOTE PROCESSING DATA RUNNING...." }
                    onEvent(ConnectorSyncEvent.RemoteProcessing)
                    decodedData to true
                }

                else -> return Result.failure(InvalidSessionTypeException())
            }

            val (sessionResult, handled) = result.getOrElse { err -> return Result.failure(err) }

            val eventOut = when (sessionResult) {
                is BLESyncSession.SyncSessionSuccessfulAck if (device != null) ->
                    ConnectorSyncEvent.FullDuplexCompleted(device, sessionResult.sessionId)

                is BLESyncSession.SyncSessionFailed -> ConnectorSyncEvent.SyncFailed(sessionResult.reason.name)
                is BLESyncSession.SyncPacketTransition if (device != null) -> {
                    val isHalfDone = sessionResult.prevType == BLESyncDataType.CONTENT &&
                        sessionResult.newType == BLESyncDataType.METADATA
                    if (isHalfDone) ConnectorSyncEvent.HalfDuplexCompleted(device)
                    else null
                }

                else -> null
            }
            if (eventOut != null) onEvent(eventOut)
            Result.success(handled)
        } catch (_: SerializationException) {
            Logger.e(tag = TAG) { "INVALID DATA RECEIVED CANNOT DECODE IT" }
            Result.failure(InvalidPayloadDataException())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "UNKNOWN EXCEPTION" }
            Result.failure(e)
        }
    }


    suspend inline fun onDataPacketEnd(
        data: BLESyncSession.BLESyncDataPacketEnd,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<SessionResult> = runCatching {

        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(
            BLESyncSession.SyncPacketProcessing(data.sessionId),
        )
        onWriteBytes(bytes)

        Logger.d(tag = TAG) { "PACKET END MARKER RECEIVED TYPE:${data.type}" }
        val result = inPayloadManager.processData(data.sessionId).getOrThrow()

        Logger.d(tag = TAG) { "PROCESSED RESULT:$result" }
        if (result is SyncDataPayload.Outgoing) outPayloadManager.prepareChunks(result)

        // handle the result
        val transitionPacket = when (result) {
            is SyncDataPayload.ContentPayload if (data.type == BLESyncDataType.CONTENT_REQUEST) ->
                BLESyncSession.SyncPacketTransition(
                    prevType = BLESyncDataType.CONTENT_REQUEST,
                    newType = BLESyncDataType.CONTENT,
                    sessionId = data.sessionId,
                )

            is SyncDataPayload.ContentIdsQuery if (data.type == BLESyncDataType.METADATA) ->
                BLESyncSession.SyncPacketTransition(
                    prevType = BLESyncDataType.METADATA,
                    newType = BLESyncDataType.CONTENT_REQUEST,
                    sessionId = data.sessionId,
                )

            is SyncDataPayload.SyncSessionSuccess -> {
                val transition = BLESyncSession.SyncSessionSuccessful(data.sessionId)
                val packetBytes = protoBuf.encodeToByteArray<BLESyncSession>(transition)
                return@runCatching transition to onWriteBytes(packetBytes)
            }

            else -> throw InvalidPayloadDataException()
        }

        val packetBytes = protoBuf.encodeToByteArray<BLESyncSession>(transitionPacket)
        return@runCatching transitionPacket to onWriteBytes(packetBytes)
    }


    suspend inline fun onPacketTransition(
        data: BLESyncSession.SyncPacketTransition,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<SessionResult> {
        Logger.d(tag = TAG) { "PACKET TYPE TRANSITION TO ${data.newType} FROM :${data.prevType}" }

        return runCatching {
            val responseData = when {
                // if the is request send ack
                data.isRequested -> {
                    // clear the buffer and send ack
                    inPayloadManager.clearBuffer()
                    outPayloadManager.reset()
                    Logger.w(tag = TAG) { "REQUESTED TRANSITION REQUEST RESPONDING WITH ACK" }
                    data.copy(isRequested = false, isAck = true)
                }

                // if no ack flag found
                !data.isAck -> {
                    Logger.w(tag = TAG) { "MISSING ACK FLAG STOPPING SYNC SESSION" }
                    BLESyncSession.SyncSessionFailed(
                        reason = BLESyncFailedReason.MISSING_FLAG,
                        isCritical = true,
                        sessionId = data.sessionId,
                    )
                }

                // if no chunk data present
                !outPayloadManager.getHasMoreChunks() -> {
                    // send we are done with sending metadata packet
                    Logger.w(tag = TAG) { "NO MORE CHUNKS TO WORK WITH SESSION DATA PACKET END" }
                    BLESyncSession.BLESyncDataPacketEnd(type = data.newType, sessionId = data.sessionId)
                }

                else -> {
                    // now send the response
                    val chunkResult = outPayloadManager.getNextChunk()
                    val chunk = chunkResult.getOrElse { err ->
                        Logger.w(tag = TAG, throwable = err) { "A CHUNK OF DATA SHOULD BE PRESENT" }
                        val session = BLESyncSession.SyncSessionFailed(
                            reason = BLESyncFailedReason.INVALID_STATE,
                            isCritical = true,
                            sessionId = data.sessionId,
                        )
                        val bytes = protoBuf.encodeToByteArray<BLESyncSession>(session)
                        onWriteBytes(bytes)
                        return@runCatching session to onWriteBytes(bytes)
                    }

                    // now send the response
                    BLESyncSession.BLESyncDataPacket(
                        type = BLESyncDataType.CONTENT_REQUEST,
                        sequenceNumber = chunk.seqNumber,
                        payload = chunk.payload,
                        sessionId = data.sessionId,
                    )
                }
            }
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(responseData)
            responseData to onWriteBytes(bytes)
        }
    }

    suspend inline fun onDataPacketReceived(
        data: BLESyncSession.BLESyncDataPacket,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<SessionResult> {
        Logger.d(tag = TAG) { "RECEIVED PACKET DATA FROM OTHER DEVICE TYPE:${data.type}" }

        return runCatching {
            inPayloadManager.addIncomingPayloadChunk(data.sequenceNumber, data.payload)
            val response = BLESyncSession.BLESyncDataAck(
                type = data.type,
                sequenceNumber = data.sequenceNumber,
                sessionId = data.sessionId,
            )
            val sessionData = protoBuf.encodeToByteArray<BLESyncSession>(response)
            response to onWriteBytes(sessionData)
        }
    }

    suspend inline fun onDataPacketACKReceived(
        data: BLESyncSession.BLESyncDataAck,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<SessionResult> {
        Logger.d(tag = TAG) { "RECEIVED PACKET ACK DATA FROM OTHER DEVICE" }
        // mark the payload as consumed
        outPayloadManager.markChunkAck(data.sequenceNumber)

        if (!outPayloadManager.getHasMoreChunks()) {
            // send we are done with sending metadata packet
            return runCatching {
                val response = BLESyncSession.BLESyncDataPacketEnd(type = data.type, sessionId = data.sessionId)
                val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
                response to onWriteBytes(bytes)
            }
        }
        val chunkResult = outPayloadManager.getNextChunk()
        // we have a block
        val chunk = chunkResult.getOrElse { err ->
            Logger.w(tag = TAG, throwable = err) { "ISSUE WITH NEXT CHUNK" }
            return Result.failure(err)
        }
        val response = BLESyncSession.BLESyncDataPacket(
            type = data.type,
            sequenceNumber = chunk.seqNumber,
            payload = chunk.payload,
            sessionId = data.sessionId,
        )
        // now send the response
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(response)
            response to onWriteBytes(bytes)
        }
    }

    suspend inline fun onSessionStartACK(
        response: BLESyncSession.SyncSessionStartAck,
        onWriteBytes: suspend (ByteArray) -> Boolean,
    ): Result<SessionResult> {
        // response ack flag should be true
        if (!response.isAck) return Result.failure(SyncStarkNotAckException())

        outPayloadManager.prepareChunks(SyncDataPayload.Metadata).getOrElse { err ->
            Logger.w(tag = TAG) { "CANNOT PREPARE THE BLOCKS" }
            return Result.failure(err)
        }

        // chunks should be probably ready by now
        if (!outPayloadManager.getHasMoreChunks()) return Result.failure(EmptyPayloadException())
        val chunk = outPayloadManager.getNextChunk()
            .getOrElse { err ->
                Logger.w(tag = TAG, throwable = err) { "ISSUE WITH NEXT CHUNK" }
                return Result.failure(err)
            }
        // we have a block
        val responseData = BLESyncSession.BLESyncDataPacket(
            type = BLESyncDataType.METADATA,
            sequenceNumber = chunk.seqNumber,
            payload = chunk.payload,
            sessionId = response.sessionId,
        )
        Logger.d(tag = TAG) { "SENDING FIRST BLOCK OF METADATA CHUNKS" }
        // now send the response
        return runCatching {
            val bytes = protoBuf.encodeToByteArray<BLESyncSession>(responseData)
            responseData to onWriteBytes(bytes)
        }
    }

    suspend inline fun onEnabledDisabledCCCDescriptor(
        address: String,
        characteristicId: Uuid,
        bytes: ByteArray,
        onWriteBytes: suspend (ByteArray) -> Boolean,
        onToggleNotification: suspend (characteristics: Uuid, enable: Boolean) -> Boolean,
    ) {
        val isEnabled = bytes.btDescriptorsNotificationOrIndicationEnabled

        when (characteristicId) {
            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID if isEnabled -> {
                // thus notification is turned on successfully
                val outgoingData = lock.withLock { handShakeDataMap[address] } ?: return
                val syncWrite = protoBuf.encodeToByteArray<BLESyncHandshakeData.AdvertiseResponseData>(outgoingData)
                val response = onWriteBytes(syncWrite)
                Logger.d(tag = TAG) { "WRITING ADVERTISING RESPONSE CHARACTERISTICS IS_SUCCESS:$response" }
            }

            BLEConstants.PROXIMITY_SYNC_CHARACTERISTICS_ID -> {
                lock.withLock {
                    val isNotificationOn = hadShakeNotificationMap[address] ?: false
                    if (!isNotificationOn) return
                    hadShakeNotificationMap.remove(address)
                }
                Logger.d(tag = TAG) { "TURNING OFF HANDSHAKE NOTIFICATION AND TURNING ON DATA NOTIFICATION" }

                onToggleNotification(BLEConstants.SYNC_DATA_CHARACTERISTICS_ID, true)
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID if isEnabled -> {
                Logger.d(tag = TAG) { "STARTING CHARACTERISTICS NOTIFICATION TURNED ON" }
                lock.withLock {
                    val sessionId = Uuid.random()
                    currentSessionId = sessionId
                    val sendData = BLESyncSession.SyncSessionStart(sessionId = sessionId)
                    val bytesToSend = protoBuf.encodeToByteArray<BLESyncSession>(sendData)
                    onWriteBytes(bytesToSend)
                }
            }

            BLEConstants.SYNC_DATA_CHARACTERISTICS_ID -> Logger.d(tag = TAG) { "SYNCING IS DONE NOW SYNC NOTIFICATION ARE DISMISSED" }
            else -> {
                val text = if (isEnabled) "ENABLED" else "DISABLED"
                Logger.d(tag = TAG) { "GATT NOTIFICATION $text  FOR CHARACTERISTICS :${characteristicId}" }
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
