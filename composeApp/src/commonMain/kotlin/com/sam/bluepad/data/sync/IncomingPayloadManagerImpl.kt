package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.data.sync.mappers.toSyncContent
import com.sam.bluepad.data.sync.mappers.toSyncMetaDataList
import com.sam.bluepad.domain.sync.InPayloadManager
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.use_cases.BytesEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

private const val TAG = "SYNC_IN_PAYLOAD"

class IncomingPayloadManagerImpl private constructor(
    private val protoBuf: ProtoBuf,
    private val syncManager: SyncManager,
    private val encoder: BytesEncoder,
    private val timezone: TimeZone,
) : InPayloadManager {

    constructor(protoBuf: ProtoBuf, syncManager: SyncManager, encoder: BytesEncoder) : this(
        protoBuf = protoBuf,
        syncManager = syncManager,
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
        val decoded = protoBuf.decodeFromByteArray<SyncPayloadSequence>(data)

        return try {
            handleDataProcessing(decoded)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(TAG, e) { "FAILED TO EXECUTE DECODING" }
            Result.failure(e)
        } finally {
            clearBuffer()
        }
    }

    private suspend fun handleDataProcessing(sequence: SyncPayloadSequence): Result<SyncDataPayload.ProcessedResult> {
        return when (sequence) {
            is SyncPayloadSequence.MetaData -> {
                // process the metadata and provide content id query
                val metadata = sequence.toSyncMetaDataList(timezone)
                val result = syncManager.findChangedItems(metadata)
                val uuids = result.getOrElse { err -> return Result.failure(err) }
                Result.success(SyncDataPayload.ContentIdsQuery(uuids))
            }

            is SyncPayloadSequence.ContentRequests -> {
                // so take the content ids and query the content and send the content
                val ids = sequence.data.map { it.contentId }
                val results = syncManager.fetchContentForExchange(ids)
                val sketches = results.getOrElse { err -> return Result.failure(err) }
                Result.success(SyncDataPayload.ContentPayload(sketches))
            }

            is SyncPayloadSequence.Content -> {
                // sync manager now handles the content data
                val data = sequence.toSyncContent(timezone)
                val results = syncManager.saveSyncContent(data)
                results.getOrElse { err -> return Result.failure(err) }
                Result.success(SyncDataPayload.NoAction)
            }
        }
    }

    override fun clearBuffer() {
        _incomingData.clear()
        Logger.d(TAG) { "BUFFER DATA IS CLEARED" }
    }

    private class BufferEmptyException : Exception("Buffer is empty")
}
