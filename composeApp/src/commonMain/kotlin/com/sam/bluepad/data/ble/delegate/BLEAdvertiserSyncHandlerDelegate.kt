package com.sam.bluepad.data.ble.delegate

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.exceptions.BLEAdvertiserException
import com.sam.bluepad.data.ble.exceptions.BLEConnectorException
import com.sam.bluepad.data.sync.dto.BLESyncCompletionReason
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.exceptions.InvalidDeviceException
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class BLEAdvertiserSyncHandlerDelegate(
    private val protoBuf: ProtoBuf,
    private val inPayloadManager: InPayloadManager,
    private val outPayloadManager: OutPayloadManager,
    private val dispatchers: PlatformDispatcherProvider,
) {

    suspend fun handleSyncDataWriteRequest(
        value: ByteArray,
        onNotify: suspend (ByteArray) -> Boolean,
        onEvent: (AdvertiserSyncEvent) -> Unit,
        onReadDevice: suspend () -> ExternalDeviceModel?,
    ): Result<Unit> = try {
        // read the request
        val requestData = withContext(dispatchers.io) {
            protoBuf.decodeFromByteArray<BLESyncSession>(value)
        }

        Logger.d(tag = TAG) { "SYNC DATA RECEIVED TYPE: ${requestData::class.simpleName} | BLOCK_SIZE:${value.size}" }

        val responseResult = when (requestData) {
            is BLESyncSession.SyncSessionStart -> {
                val device = onReadDevice()
                    ?: return Result.failure(InvalidDeviceException())
                onEvent(AdvertiserSyncEvent.SyncStarted(device))
                manageSessionStartReq(requestData)
            }

            is BLESyncSession.BLESyncDataPacket -> manageSyncSessionDataPacket(requestData)
            is BLESyncSession.BLESyncDataAck -> manageSyncSessionDataPacketAck(requestData)
            is BLESyncSession.BLESyncDataPacketEnd -> markSyncSessionPacketEnded(requestData, onNotify)
            is BLESyncSession.SyncPacketTransition -> {
                if (requestData.prevType == BLESyncDataType.CONTENT && requestData.newType == BLESyncDataType.METADATA) {
                    val device = onReadDevice() ?: return Result.failure(InvalidDeviceException())
                    onEvent(AdvertiserSyncEvent.HalfDuplexCompleted(device))
                }
                checkTransitionAckAndSendDataPacket(requestData)
            }

            is BLESyncSession.SyncSessionSuccessful -> {
                val device = onReadDevice()
                    ?: return Result.failure(InvalidDeviceException())
                onEvent(AdvertiserSyncEvent.FullDuplexCompleted(device, requestData.sessionId))
                onSyncSuccessful(requestData)
            }

            is BLESyncSession.SyncPacketProcessing -> {
                Logger.d(tag = TAG) { "REMOTE PROCESSING DATA RUNNING...." }
                onEvent(AdvertiserSyncEvent.RemoteProcessing)
                return Result.success(Unit)
            }

            is BLESyncSession.SyncSessionFailed -> {
                Logger.d(tag = TAG) { "SYNC SESSION FAILED" }
                onEvent(AdvertiserSyncEvent.SyncFailed(requestData.reason.name))
                return Result.success(Unit)
            }

            else -> throw BLEAdvertiserException.UnsupportedSyncSessionException(requestData)
        }
            .getOrThrow()

        val bytes = withContext(dispatchers.io) {
            protoBuf.encodeToByteArray<BLESyncSession>(responseResult)
        }
        val finalWriteOp = onNotify(bytes)
        if (!finalWriteOp) onEvent(AdvertiserSyncEvent.SyncFailed("Cannot notify the connector"))
        Result.success(Unit)
    } catch (_: SerializationException) {
        Logger.e(tag = TAG) { "INVALID DATA RECEIVED CANNOT DECODE IT" }
        Result.failure(BLEConnectorException.InvalidPayloadDataException())
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Logger.e(tag = TAG, throwable = e) { "UNKNOWN EXCEPTION" }
        Result.failure(e)
    }

    private fun onSyncSuccessful(payload: BLESyncSession.SyncSessionSuccessful)
        : Result<BLESyncSession> = runCatching {
        BLESyncSession.SyncSessionSuccessfulAck(payload.sessionId, reason = payload.reason)
    }

    private suspend fun manageSyncSessionDataPacketAck(data: BLESyncSession.BLESyncDataAck)
        : Result<BLESyncSession> = runCatching {

        Logger.d(tag = TAG) { "RECEIVED A DATA PACKET ACK TYPE:${data.type} SESSION_ID :${data.sessionId}" }

        // mark the payload as consumed
        outPayloadManager.markChunkAck(data.sessionId, data.sequenceNumber)

        // check if wr have more bytes that can be sent
        if (!outPayloadManager.getHasMoreChunks(data.sessionId)) {
            // send we are done with sending metadata packet
            val response = BLESyncSession.BLESyncDataPacketEnd(type = data.type, sessionId = data.sessionId)
            return@runCatching response
        }

        val chunk = outPayloadManager.getNextChunk(data.sessionId)
            .getOrElse { err ->
                Logger.w(tag = TAG, throwable = err) { "CANNOT USE NEXT CHUNK EVEN IF ITS CLEAR" }
                Logger.w(tag = TAG) { "RESPONDING WITH SYN SESSION FAILED" }
                // mark this as failed
                val response = BLESyncSession.SyncSessionFailed(
                    reason = BLESyncFailedReason.TAMPERED_DATA,
                    isCritical = true,
                    sessionId = data.sessionId,
                )
                return@runCatching response
            }

        // now send the response as the payload block
        BLESyncSession.BLESyncDataPacket(
            type = data.type,
            sequenceNumber = chunk.seqNumber,
            payload = chunk.payload,
            sessionId = data.sessionId,
        )
    }


    private suspend fun markSyncSessionPacketEnded(
        payload: BLESyncSession.BLESyncDataPacketEnd,
        onProcessing: suspend (ByteArray) -> Boolean
    ): Result<BLESyncSession> = coroutineScope {

        Logger.d(tag = TAG) { "SESSION PACKET END RECEIVED TYPE:${payload.type} SESSION_ID:${payload.sessionId}" }

        val job = launch {
            val packet = BLESyncSession.SyncPacketProcessing(payload.sessionId)
            val bytes = withContext(dispatchers.io) {
                protoBuf.encodeToByteArray<BLESyncSession>(packet)
            }
            onProcessing(bytes)
        }

        // process the payload
        val processedResult = inPayloadManager.processData(payload.sessionId)
        val result = processedResult.getOrElse { error ->
            Logger.e(tag = TAG, throwable = error) { "FAILED TO PROCESS SESSION ${payload.sessionId}" }
            val response = BLESyncSession.SyncSessionFailed(
                reason = BLESyncFailedReason.INVALID_STATE,
                isCritical = true,
                sessionId = payload.sessionId,
            )
            return@coroutineScope Result.success(response)
        }

        runCatching {
            Logger.d(tag = TAG) { "PROCESSING JOB CAN BE CANCELLED ISACTIVE: ${job.isActive} COMPLETED: ${job.isCompleted}" }
            if (job.isActive) job.cancelAndJoin()

            when (result) {
                // processing the incoming data shows that there is no changes between
                // the current device data and the other pair
                is SyncDataPayload.ContentIdsQuery if result.ids.isEmpty() -> {
                    BLESyncSession.SyncSessionSuccessful(
                        sessionId = payload.sessionId,
                        reason = BLESyncCompletionReason.CONTENT_ALREADY_SAME,
                    )
                }

                is SyncDataPayload.ContentIdsQuery if (payload.type == BLESyncDataType.METADATA) -> {
                    // LOAD THE DATA AND TRANSITION FROM METADAT TO CONTENT_REQ
                    outPayloadManager.prepareChunks(payload.sessionId, result)
                    BLESyncSession.SyncPacketTransition(
                        prevType = BLESyncDataType.METADATA,
                        newType = BLESyncDataType.CONTENT_REQUEST,
                        sessionId = payload.sessionId,
                    )
                }

                is SyncDataPayload.ContentPayload if payload.type == BLESyncDataType.CONTENT_REQUEST -> {
                    // LOAD THE DATA AND TRANSITION FROM CONTENT REQ TO CONTENT
                    outPayloadManager.prepareChunks(payload.sessionId, result)
                    // now send a transition request
                    BLESyncSession.SyncPacketTransition(
                        prevType = BLESyncDataType.CONTENT_REQUEST,
                        newType = BLESyncDataType.CONTENT,
                        sessionId = payload.sessionId,
                    )
                }

                is SyncDataPayload.SyncSessionSuccess -> {
                    // Half duplex sync done
                    Logger.d(tag = TAG) { "STARTING THE SECOND HALF-DUPLEX SYNC SESSION ID:${payload.sessionId}" }
                    // NOW WE NEED TO SEND THE METADATA FROM THE ADVERTISER
                    outPayloadManager.prepareChunks(payload.sessionId, SyncDataPayload.Metadata)
                    // now send a transition request
                    BLESyncSession.SyncPacketTransition(
                        prevType = BLESyncDataType.CONTENT,
                        newType = BLESyncDataType.METADATA,
                        sessionId = payload.sessionId,
                    )
                }

                else -> throw BLEAdvertiserException.InvalidSyncPayloadException()
            }
        }
    }

    private suspend fun checkTransitionAckAndSendDataPacket(payload: BLESyncSession.SyncPacketTransition)
        : Result<BLESyncSession> = runCatching {

        Logger.d(tag = TAG) { "PACKET TRANSITION RECEIVED SESSION_ID:${payload.sessionId}" }
        // if the transition requested send an ack
        when {
            payload.isRequested -> {
                Logger.d(tag = TAG) { "PACKET TRANSITION SENDING ACK" }
                // clear the buffer and send ack
                inPayloadManager.clearBuffer(payload.sessionId)
                outPayloadManager.reset()
                // send the same payload but with ack on
                payload.copy(isRequested = false, isAck = true)
            }

            // transition need to be ack
            !payload.isAck -> {
                Logger.w(tag = TAG) { "CANCELLING SYNC SESSION DUE TO MISSING FLAG" }
                BLESyncSession.SyncSessionFailed(
                    reason = BLESyncFailedReason.MISSING_FLAG,
                    isCritical = true,
                    sessionId = payload.sessionId,
                )
            }

            !outPayloadManager.getHasMoreChunks(payload.sessionId) -> {
                Logger.w(tag = TAG) { "CANNOT FIND ANYTHING TO SEND SENDING PACKET END" }
                // send we are done with sending metadata packet
                BLESyncSession.BLESyncDataPacketEnd(type = payload.newType, sessionId = payload.sessionId)
            }

            // now send the response
            else -> {
                val chunkResult = outPayloadManager.getNextChunk(payload.sessionId)
                // we have a block
                val chunk = chunkResult.getOrElse { err ->
                    Logger.w(tag = TAG, throwable = err) { "A CHUNK OF DATA SHOULD BE PRESENT" }
                    return@runCatching BLESyncSession.SyncSessionFailed(
                        reason = BLESyncFailedReason.INVALID_STATE,
                        isCritical = true,
                        sessionId = payload.sessionId,
                    )
                }

                // now send the response
                return@runCatching BLESyncSession.BLESyncDataPacket(
                    type = payload.newType,
                    sequenceNumber = chunk.seqNumber,
                    payload = chunk.payload,
                    sessionId = payload.sessionId,
                )
            }
        }
    }


    private suspend fun manageSyncSessionDataPacket(data: BLESyncSession.BLESyncDataPacket)
        : Result<BLESyncSession> = runCatching {

        Logger.d(tag = TAG) { "RECEIVED A DATA PACKET TYPE:${data.type} SESSION_ID:${data.sessionId}" }
        // no data
        if (data.isEmptyStart) {
            Logger.d(tag = TAG) { "NO INIAL DATA FOUND ON THE START OF SYNC FROM CONNECTOR" }
            return@runCatching BLESyncSession.SyncPacketTransition(
                prevType = null,
                newType = BLESyncDataType.METADATA,
                sessionId = data.sessionId,
                isRequested = true,
            )
        }

        // add the chunk to the payload
        inPayloadManager.addIncomingPayloadChunk(data.sessionId, data.sequenceNumber, data.payload)
        // response ack payload
        BLESyncSession.BLESyncDataAck(
            type = data.type,
            sequenceNumber = data.sequenceNumber,
            sessionId = data.sessionId,
        )
    }

    private suspend fun manageSessionStartReq(payload: BLESyncSession.SyncSessionStart)
        : Result<BLESyncSession> = runCatching {
        // Clear buffers for a new session.
        outPayloadManager.reset()
        inPayloadManager.clearBuffer(payload.sessionId)

        Logger.d(tag = TAG) { "SESSION START ACK SESSION ID:${payload.sessionId}" }

        BLESyncSession.SyncSessionStartAck(
            isAck = true,
            sessionId = payload.sessionId,
        )
    }

    fun cleanUp() {
        inPayloadManager.clearAllBuffers()
    }

    companion object {
        private const val TAG = "SERVER_METHOD_HANDLER"
    }
}
