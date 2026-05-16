package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.data.sync.mappers.toContentList
import com.sam.bluepad.data.sync.mappers.toSyncMetadataList
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync.exceptions.EmptyPayloadException
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
) : InPayloadManager {

    constructor(
        protoBuf: ProtoBuf,
        syncManager: SyncManager,
        encoder: BytesEncoder,
        repo: SketchesRepository
    ) : this(
        protoBuf = protoBuf,
        syncManager = syncManager,
        encoder = encoder,
        sketchRepository = repo,
        timezone = TimeZone.currentSystemDefault(),
    )

    private val _mutex = Mutex()
    private val _incomingData = mutableMapOf<Int, String>()

    override suspend fun addIncomingPayloadChunk(seq: Int, payload: String) = _mutex.withLock {
        if (_incomingData.containsKey(seq)) {
            Logger.w(tag = TAG) { "DUPLICATE CHUNK FOR seq=$seq, ignoring." }
            return@withLock
        }
        _incomingData[seq] = payload
    }

    override suspend fun processData(sessionId: Uuid): Result<SyncDataPayload.ProcessedResult> {

        val data = _mutex.withLock {
            _incomingData.toSortedMap().values
                .map { encoder.decodeBytes(it) }
                .fold(byteArrayOf()) { acc, bytes -> acc + bytes }
        }

        Logger.d(tag = TAG) { "PROCESSING INCOMING DATA" }

        if (data.isEmpty()) {
            Logger.w(tag = TAG) { "CANNOT PROCESS ANY DATA" }
            return Result.failure(EmptyPayloadException())
        }
        val decoded = protoBuf.decodeFromByteArray<SyncPayloadSequence>(data)

        return try {
            Result.success(handleDataProcessing(sessionId, decoded))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "FAILED TO EXECUTE DECODING" }
            Result.failure(e)
        } finally {
            withContext(NonCancellable) {
                clearBuffer()
            }
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

    override suspend fun clearBuffer() {
        _mutex.withLock {
            _incomingData.clear()
            Logger.d(tag = TAG) { "BUFFER DATA IS CLEARED" }
        }
    }
}
