package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.data.sync.mappers.toContentList
import com.sam.bluepad.data.sync.mappers.toSyncMetadataList
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.compression.ICompressionManager
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync.exceptions.IncomingDataException
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.sync.models.toContentModel
import com.sam.bluepad.domain.use_cases.BytesEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.Uuid

private const val TAG = "SYNC_IN_PAYLOAD"

class IncomingPayloadManagerImpl private constructor(
    private val protoBuf: ProtoBuf,
    private val syncManager: SyncManager,
    private val encoder: BytesEncoder,
    private val sketchRepository: SketchesRepository,
    private val timezone: TimeZone,
    private val compressor: ICompressionManager,
    private val dispatchers: PlatformDispatcherProvider,
) : InPayloadManager {

    constructor(
        protoBuf: ProtoBuf,
        syncManager: SyncManager,
        encoder: BytesEncoder,
        repo: SketchesRepository,
        compressor: ICompressionManager,
        dispatchers: PlatformDispatcherProvider
    ) : this(
        protoBuf = protoBuf,
        syncManager = syncManager,
        encoder = encoder,
        sketchRepository = repo,
        compressor = compressor,
        dispatchers = dispatchers,
        timezone = TimeZone.currentSystemDefault(),
    )

    private val _lock = Mutex()
    private val _sessions = HashMap<Uuid, SessionBuffer>()

    override suspend fun addIncomingPayloadChunk(sessionId: Uuid, seq: UInt, payload: String) {
        val session = _lock.withLock { _sessions.computeIfAbsent(sessionId) { SessionBuffer() } }
        session.mutex.withLock {
            if (session.incomingData.containsKey(seq)) {
                Logger.w(tag = TAG) { "DUPLICATE CHUNK FOR sessionId=$sessionId, seq=$seq, ignoring." }
                return@withLock
            }
            session.incomingData[seq] = payload
        }
    }

    override suspend fun processData(sessionId: Uuid): Result<SyncDataPayload.ProcessedResult> {
        val session = _sessions[sessionId] ?: return Result.failure(IncomingDataException.EmptyPayloadException())

        val rawEncodedString = session.mutex.withLock {
            if (session.incomingData.isEmpty()) return Result.failure(IncomingDataException.EmptyPayloadException())
            withContext(dispatchers.default) {
                session.incomingData.toSortedMap().values.joinToString("")
            }
        }

        Logger.d(tag = TAG) { "PROCESSING INCOMING DATA FOR SESSION: $sessionId" }

        return try {
            val data = encoder.decodeBytes(rawEncodedString)
            if (data.isEmpty()) {
                Logger.w(tag = TAG) { "DECODED DATA IS EMPTY FOR SESSION: $sessionId" }
                return Result.failure(IncomingDataException.EmptyPayloadException())
            }

            val inflatedData = withContext(dispatchers.default) { compressor.inflateBytes(data) }
            val decoded = withContext(dispatchers.io) {
                protoBuf.decodeFromByteArray<SyncPayloadSequence>(inflatedData)
            }

            Logger.d(tag = TAG) { "TOTAL BYTES TO BE RECEIVED UN-COMPRESSED :${inflatedData.size} COMPRESSED:${data.size}" }

            Result.success(handleDataProcessing(sessionId, decoded))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "FAILED TO EXECUTE DECODING FOR SESSION: $sessionId" }
            Result.failure(e)
        } finally {
            clearBuffer(sessionId)
        }
    }

    private suspend fun handleDataProcessing(
        sessionId: Uuid,
        sequence: SyncPayloadSequence
    ) = when (sequence) {
        is SyncPayloadSequence.MetaData -> {
            // process the metadata and provide content id query
            val metadata = sequence.toSyncMetadataList(timezone)
            val result = syncManager.readChangedItemsIds(metadata)
            val uuids = result.getOrThrow()
            SyncDataPayload.ContentIdsQuery(uuids)
        }

        is SyncPayloadSequence.ContentRequests -> {
            // so take the content ids and query the content and send the content
            val ids = sequence.data.map { it.contentId }
            val results = sketchRepository.readSketchesByUUID(ids)
            val sketches = results.getOrThrow()
            val payload = sketches.map { it.toContentModel() }
            SyncDataPayload.ContentPayload(payload)
        }

        is SyncPayloadSequence.Content -> {
            // sync manager now handles the content data
            val data = sequence.toContentList(timezone)
            syncManager.performSyncResultsOperation(sessionId = sessionId, data)
            SyncDataPayload.SyncSessionSuccess(sessionId)
        }

    }

    override suspend fun clearBuffer(sessionId: Uuid) {
        withContext(NonCancellable) {
            val session = _lock.withLock { _sessions.remove(sessionId) } ?: return@withContext
            session.mutex.withLock {
                session.incomingData.clear()
            }
        }
        Logger.d(tag = TAG) { "BUFFER DATA CLEARED FOR SESSION: $sessionId" }

    }

    override fun clearAllBuffers() {
        _sessions.clear()
        Logger.d(tag = TAG) { "ALL SESSION BUFFERS CLEARED" }
    }

    private class SessionBuffer {
        val mutex = Mutex()
        val incomingData = mutableMapOf<UInt, String>()
    }
}
