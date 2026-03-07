package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.use_cases.BytesEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.Uuid

private const val TAG = "SYNC_IN_PAYLOAD"

class IncomingPayloadManagerImpl private constructor(
    private val protoBuf: ProtoBuf,
    private val repo: SketchesRepository,
    private val encoder: BytesEncoder,
    private val timezone: TimeZone,
) : InPayloadManager {

    constructor(protoBuf: ProtoBuf, repo: SketchesRepository, encoder: BytesEncoder) : this(
        protoBuf = protoBuf,
        repo = repo,
        encoder = encoder,
        timezone = TimeZone.currentSystemDefault()
    )

    private val _mutex = Mutex()
    private val _incomingData = mutableMapOf<Int, String>()

    override suspend fun addIncomingPayloadChunk(seq: Int, payload: String) = _mutex.withLock {
        if (_incomingData.containsKey(seq)) {
            Logger.w(TAG) { "DUPLICATE CHUNK FOR seq=$seq, ignoring." }
            return@withLock
        }
        _incomingData[seq] = payload
    }

    override suspend fun processData(): Result<SyncDataPayload.ProcessedResult> {

        val data = _mutex.withLock {
            _incomingData.toSortedMap().values
                .map { encoder.decodeBytes(it) }
                .fold(byteArrayOf()) { acc, bytes -> acc + bytes }
        }

        Logger.d(TAG) { "PROCESSING INCOMING DATA" }

        if (data.isEmpty()) return Result.failure(BufferEmptyException())

        return try {
            when (val decoded = protoBuf.decodeFromByteArray<SyncPayloadSequence>(data)) {
                is SyncPayloadSequence.MetaData -> {
                    // process the metadata and return the applicable uuids who's content changed
                    val result = processIncomingMetadata(decoded)
                    val uuids = result.getOrElse { err -> return Result.failure(err) }
                    Result.success(SyncDataPayload.ContentQuery(uuids))
                }

                is SyncPayloadSequence.ContentQueries -> {
                    // returns the decoded content queries and return uuids for which content payload
                    // need to be prepared
                    Result.success(SyncDataPayload.ContentPayload(decoded.data.map { it.queryId }))
                }

                is SyncPayloadSequence.Content -> {
                    processIncomingData(decoded)
                    Result.success(SyncDataPayload.NoAction)
                }
            }

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(TAG, e) { "FAILED TO EXECUTE DECODING" }
            Result.failure(e)
        } finally {
            clearBuffer()
        }
    }


    private suspend fun processIncomingData(sequence: SyncPayloadSequence.Content): Result<Unit> {
        // TODO: SAVE THE DATA
        return Result.success(Unit)
    }

    private suspend fun processIncomingMetadata(sequence: SyncPayloadSequence.MetaData): Result<List<Uuid>> {

        val sketches = repo.readSketchesByUUID(sequence.data.map { it.itemId })
            .getOrElse { return Result.failure(it) }
            .associateBy { it.id }

        return coroutineScope {
            val finalList = sequence.data.mapNotNull { metadata ->
                ensureActive()
                val model = sketches[metadata.itemId] ?: return@mapNotNull null

                val changed = metadata.version > model.version ||
                        metadata.contentHash != model.contentHash ||
                        metadata.lastModified > model.modifiedAt.toInstant(timezone) ||
                        metadata.title != model.title
                if (!changed) return@mapNotNull null
                metadata.itemId
            }.distinct()
            Result.success(finalList)
        }
    }


    override fun clearBuffer() {
        _incomingData.clear()
        Logger.d(TAG) { "BUFFER DATA IS CLEARED" }
    }

    private class BufferEmptyException : Exception("Buffer is empty")
}
