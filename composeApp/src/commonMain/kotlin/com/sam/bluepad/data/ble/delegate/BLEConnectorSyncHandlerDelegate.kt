package com.sam.bluepad.data.ble.delegate

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.exceptions.BLEConnectorException
import com.sam.bluepad.data.sync.dto.BLESyncDataType
import com.sam.bluepad.data.sync.dto.BLESyncFailedReason
import com.sam.bluepad.data.sync.dto.BLESyncSession
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
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
import kotlin.uuid.Uuid

class BLEConnectorSyncHandlerDelegate(
    private val protoBuf: ProtoBuf,
    private val outPayloadManager: OutPayloadManager,
    private val inPayloadManager: InPayloadManager,
    private val dispatcher: PlatformDispatcherProvider,
) {

    suspend fun handleSyncDataNotification(
        characteristicId: Uuid,
        value: ByteArray,
        onEvent: (ConnectorSyncEvent) -> Unit,
        onWriteBytes: suspend (ByteArray) -> Boolean,
        onReadDevice: suspend () -> ExternalDeviceModel?,
        onToggleNotification: suspend (characteristics: Uuid, enable: Boolean) -> Boolean,
    ): Result<Unit> = try {

        val requestData = withContext(dispatcher.io) {
            protoBuf.decodeFromByteArray<BLESyncSession>(value)
        }

        Logger.d(tag = TAG) { "SYNC DATA RECEIVED TYPE: ${requestData::class.simpleName} | BLOCK_SIZE:${value.size}" }

        val result = when (requestData) {
            is BLESyncSession.SyncSessionStartAck -> {
                val device = onReadDevice()
                    ?: return Result.failure(InvalidDeviceException())
                onEvent(ConnectorSyncEvent.SyncStarted(device))
                respondSessionStartAck(requestData)
            }

            is BLESyncSession.BLESyncDataPacket -> onDataPacketReceived(requestData)
            is BLESyncSession.BLESyncDataAck -> onDataPacketACKReceived(requestData)
            is BLESyncSession.BLESyncDataPacketEnd -> onDataPacketEnd(requestData, onWriteBytes)
            is BLESyncSession.SyncPacketTransition -> {
                if (requestData.prevType == BLESyncDataType.CONTENT && requestData.newType == BLESyncDataType.METADATA) {
                    val device = onReadDevice() ?: return Result.failure(InvalidDeviceException())
                    onEvent(ConnectorSyncEvent.HalfDuplexCompleted(device))
                }
                // then handle the transition packets
                onPacketTransition(requestData)
            }

            is BLESyncSession.SyncSessionSuccessfulAck -> {
                Logger.d(tag = TAG) { "SYNC SUCCESSFUL ACK-ED" }
                val device = onReadDevice() ?: return Result.failure(InvalidDeviceException())
                val event = ConnectorSyncEvent.FullDuplexCompleted(device, requestData.sessionId)
                onEvent(event)
                return Result.success(Unit)
            }

            is BLESyncSession.SyncSessionFailed -> {
                Logger.d(tag = TAG) { "SYNC SESSION FAILED" }
                onEvent(ConnectorSyncEvent.SyncFailed(requestData.reason.name))
                val result = onToggleNotification(characteristicId, false)
                if (!result) onEvent(ConnectorSyncEvent.SyncFailed("Unable to activate notification"))
                return Result.success(Unit)
            }

            is BLESyncSession.SyncPacketProcessing -> {
                Logger.d(tag = TAG) { "REMOTE PROCESSING DATA RUNNING...." }
                onEvent(ConnectorSyncEvent.RemoteProcessing)
                return Result.success(Unit)
            }

            else -> return Result.failure(BLEConnectorException.InvalidSessionTypeException())
        }
            // if we got any issues just throw it
            .getOrThrow()

        val bytes = withContext(dispatcher.io) {
            protoBuf.encodeToByteArray<BLESyncSession>(result)
        }
        val finalWriteOp = onWriteBytes(bytes)
        if (!finalWriteOp) onEvent(ConnectorSyncEvent.SyncFailed("Write operation failed"))

        Result.success(Unit)
    } catch (_: SerializationException) {
        Logger.e(tag = TAG) { "INVALID DATA RECEIVED CANNOT DECODE IT" }
        Result.failure(BLEConnectorException.InvalidPayloadDataException())
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Logger.e(tag = TAG, throwable = e) { "UNKNOWN EXCEPTION" }
        Result.failure(e)
    }


    private suspend fun onDataPacketEnd(
        data: BLESyncSession.BLESyncDataPacketEnd,
        onProcessing: suspend (ByteArray) -> Boolean
    ) = coroutineScope {
        // an independent coroutine to indicate  processing has started
        val job = launch {
            val packet = BLESyncSession.SyncPacketProcessing(data.sessionId)
            val bytes = withContext(dispatcher.io) {
                protoBuf.encodeToByteArray<BLESyncSession>(packet)
            }
            onProcessing(bytes)
        }

        Logger.d(tag = TAG) { "PACKET END MARKER RECEIVED TYPE:${data.type}" }
        val result = inPayloadManager.processData(data.sessionId).getOrElse { error ->
            Logger.e(tag = TAG, throwable = error) { "FAILED TO PROCESS SESSION ${data.sessionId}" }
            val response = BLESyncSession.SyncSessionFailed(
                reason = BLESyncFailedReason.INVALID_STATE,
                isCritical = true,
                sessionId = data.sessionId,
            )
            return@coroutineScope Result.success(response)
        }

        Logger.d(tag = TAG) { "PROCESSED RESULT:$result" }
        if (result is SyncDataPayload.Outgoing) outPayloadManager.prepareChunks(result)

        runCatching {
            // handle the result
            Logger.d(tag = TAG) { "PROCESSING JOB CAN BE CANCELLED ISACTIVE: ${job.isActive} COMPLETED: ${job.isCompleted}" }
            if (job.isActive) job.cancelAndJoin()

            // result is ready if the processing job is still waiting just stop it
            when (result) {
                is SyncDataPayload.ContentPayload if (data.type == BLESyncDataType.CONTENT_REQUEST) -> {
                    BLESyncSession.SyncPacketTransition(
                        prevType = BLESyncDataType.CONTENT_REQUEST,
                        newType = BLESyncDataType.CONTENT,
                        sessionId = data.sessionId,
                    )
                }

                is SyncDataPayload.ContentIdsQuery if (data.type == BLESyncDataType.METADATA) -> {
                    BLESyncSession.SyncPacketTransition(
                        prevType = BLESyncDataType.METADATA,
                        newType = BLESyncDataType.CONTENT_REQUEST,
                        sessionId = data.sessionId,
                    )
                }

                is SyncDataPayload.SyncSessionSuccess -> {

                    val transition = BLESyncSession.SyncSessionSuccessful(data.sessionId)
                    return@runCatching transition

                }

                else -> throw BLEConnectorException.InvalidPayloadDataException()
            }
        }
    }


    private suspend fun onPacketTransition(data: BLESyncSession.SyncPacketTransition) = runCatching {
        Logger.d(tag = TAG) { "PACKET TYPE TRANSITION TO ${data.newType} FROM :${data.prevType}" }
        when {
            // if the is request send ack
            data.isRequested -> {
                Logger.w(tag = TAG) { "REQUESTED TRANSITION REQUEST RESPONDING WITH ACK" }
                // clear the buffer
                inPayloadManager.clearBuffer()
                outPayloadManager.reset()
                // send it back with same ack
                data.copy(isRequested = false, isAck = true)
            }

            !data.isAck -> {
                // if no ack flag found
                Logger.w(tag = TAG) { "MISSING ACK FLAG STOPPING SYNC SESSION" }
                BLESyncSession.SyncSessionFailed(
                    reason = BLESyncFailedReason.MISSING_FLAG,
                    isCritical = true,
                    sessionId = data.sessionId,
                )
            }

            !outPayloadManager.getHasMoreChunks() -> {
                // Checks when we call for packets and we dont have any more
                // so mark as packet end and no more data sending
                //  we are done with sending metadata packet
                Logger.w(tag = TAG) { "NO MORE CHUNKS TO WORK WITH SESSION DATA PACKET END" }
                BLESyncSession.BLESyncDataPacketEnd(type = data.newType, sessionId = data.sessionId)
            }

            else -> {
                // now send the response
                val chunkResult = outPayloadManager.getNextChunk()
                val chunk = chunkResult.getOrElse { err ->
                    Logger.w(tag = TAG, throwable = err) { "A CHUNK OF DATA SHOULD BE PRESENT" }
                    return@runCatching BLESyncSession.SyncSessionFailed(
                        reason = BLESyncFailedReason.INVALID_STATE,
                        isCritical = true,
                        sessionId = data.sessionId,
                    )
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
    }

    private suspend fun onDataPacketReceived(data: BLESyncSession.BLESyncDataPacket) = runCatching {
        Logger.d(tag = TAG) { "RECEIVED PACKET DATA FROM OTHER DEVICE TYPE:${data.type}" }

        inPayloadManager.addIncomingPayloadChunk(data.sequenceNumber, data.payload)
        val response = BLESyncSession.BLESyncDataAck(
            type = data.type,
            sequenceNumber = data.sequenceNumber,
            sessionId = data.sessionId,
        )
        Logger.d(tag = TAG) { "SENDING PACKET ACK FOR :${data.sequenceNumber}" }
        response
    }

    private suspend fun onDataPacketACKReceived(data: BLESyncSession.BLESyncDataAck) = runCatching {
        Logger.d(tag = TAG) { "RECEIVED PACKET ACK DATA FROM OTHER DEVICE" }
        // mark the payload as consumed
        outPayloadManager.markChunkAck(data.sequenceNumber)

        if (!outPayloadManager.getHasMoreChunks()) {
            Logger.d(tag = TAG) { "NO MORE CHUNKS FOUND MARKING PACKET END" }
            // send we are done with sending metadata packet
            val response = BLESyncSession.BLESyncDataPacketEnd(type = data.type, sessionId = data.sessionId)
            return@runCatching response
        }
        val chunkResult = outPayloadManager.getNextChunk()
        // we have a block
        val chunk = chunkResult.getOrElse { err ->
            Logger.w(tag = TAG, throwable = err) { "ISSUE WITH NEXT CHUNK" }
            throw err
        }
        val response = BLESyncSession.BLESyncDataPacket(
            type = data.type,
            sequenceNumber = chunk.seqNumber,
            payload = chunk.payload,
            sessionId = data.sessionId,
        )
        Logger.d(tag = TAG) { "SENDING DATA PACKET FOR SEQ NUMBER :${chunk.seqNumber}" }
        response
    }

    private suspend fun respondSessionStartAck(request: BLESyncSession.SyncSessionStartAck) = runCatching {
        // Missing acknowledgement flag
        if (!request.isAck) throw BLEConnectorException.SyncStarkNotAckException()

        // first prepare chunks for metadata
        outPayloadManager.prepareChunks(SyncDataPayload.Metadata)
            .getOrElse { err ->
                Logger.w(tag = TAG, throwable = err) { "CANNOT PREPARE THE BLOCKS" }
                throw err
            }

        // check if we have chunks ?
        if (!outPayloadManager.getHasMoreChunks()) {
            // we might not have any chunks as initial payload can be empty
            Logger.d(tag = TAG) { "CANNOT FIND ANY BLOCK FOR START SENDING EMPTY START" }
            val responseData = BLESyncSession.BLESyncDataPacket(
                type = BLESyncDataType.METADATA,
                isEmptyStart = true,
                sessionId = request.sessionId,
            )
            Logger.d(tag = TAG) { "SENDING THE DATA PACKET BUT WITH ISEMPY FLAG ON" }
            return@runCatching responseData
        }
        // get the chunk
        val chunk = outPayloadManager.getNextChunk().getOrElse { err ->
            Logger.w(tag = TAG, throwable = err) { "ISSUE WITH NEXT CHUNK" }
            throw err
        }

        // we have a block
        Logger.d(tag = TAG) { "FIRST BLOCK OF METADATA IS READY" }
        val responseData = BLESyncSession.BLESyncDataPacket(
            type = BLESyncDataType.METADATA,
            sequenceNumber = chunk.seqNumber,
            payload = chunk.payload,
            sessionId = request.sessionId,
        )
        Logger.d(tag = TAG) { "SENDING FIRST BLOCK OF METADATA CHUNKS" }
        // now send the response
        responseData
    }

    companion object {
        private const val TAG = "SYNC_DEVICE_CONNECTION_DELEGATE"
    }
}
