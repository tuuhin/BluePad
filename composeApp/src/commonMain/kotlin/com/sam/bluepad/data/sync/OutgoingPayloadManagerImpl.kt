package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.SyncDataFrame
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.data.sync.mappers.toSyncContentFrame
import com.sam.bluepad.data.sync.mappers.toSyncMetadataFrame
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.models.FragmentedDataBlock
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.use_cases.BytesEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
class OutgoingPayloadManagerImpl private constructor(
    private val protoBuf: ProtoBuf,
    private val repo: SketchesRepository,
    private val encoder: BytesEncoder,
    private val timezone: TimeZone,
) : OutPayloadManager {

    constructor(protoBuf: ProtoBuf, repo: SketchesRepository, encoder: BytesEncoder) : this(
        protoBuf = protoBuf,
        repo = repo,
        encoder = encoder,
        timezone = TimeZone.currentSystemDefault()
    )

    private val _lock = Mutex()

    // outgoing data
    private val _dataQueue = ConcurrentLinkedQueue<String>()
    private val _ackMap = ConcurrentHashMap<Int, Boolean>()

    // incoming data
    private val _sequenceNumber = AtomicInt(0)
    private val _readSinceLastAck = AtomicInt(0)

    override suspend fun prepareChunks(type: SyncDataPayload.Outgoing): Result<Unit> {
        reset()
        // convert all of them into sync data metadata
        val payloadSeq = when (type) {
            SyncDataPayload.Metadata -> {
                val sketchesResult = repo.readAllSketches()
                val sketches = sketchesResult.getOrElse { err -> return Result.failure(err) }
                val items = sketches.map { model -> model.toSyncMetadataFrame(timezone) }
                SyncPayloadSequence.MetaData(items)
            }

            is SyncDataPayload.ContentQuery -> {
                val items = type.ids.map { id -> SyncDataFrame.ContentIDQuery(id) }
                SyncPayloadSequence.ContentQueries(items)
            }

            is SyncDataPayload.ContentPayload -> {
                val sketchesResult = repo.readSketchesByUUID(type.ids)
                val sketches = sketchesResult.getOrElse { err -> return Result.failure(err) }
                val items = sketches.map { model -> model.toSyncContentFrame(timezone) }
                SyncPayloadSequence.Content(items)
            }
        }
        return runCatching { chunkAndSequencePayload(payloadSeq) }
    }


    override suspend fun getHasMoreChunks(): Boolean = _lock.withLock {
        _dataQueue.isNotEmpty()
    }

    override suspend fun getNextChunk(): Result<FragmentedDataBlock> = runCatching {
        // an acknowledgement is required to continue
        if (_readSinceLastAck.load() >= MAX_READ_WITHOUT_ACK)
            return Result.failure(Exception("WINDOW LIMIT REACHED NEED ACK"))

        // update the sequence number and generate the value
        val poll = _lock.withLock {
            _dataQueue.poll()
                ?: return Result.failure(Exception("NO MORE DATA TO PROCESS"))
        }
        val nextSeqNumber = _sequenceNumber.incrementAndFetch()

        // keep the poll data in the hash map
        _ackMap[nextSeqNumber] = false
        FragmentedDataBlock(seqNumber = nextSeqNumber, payload = poll)
    }

    override suspend fun markChunkAck(seq: Int) = _lock.withLock {
        _ackMap[seq] = true
    }


    private suspend fun chunkAndSequencePayload(payload: SyncPayloadSequence) {
        val bytes = protoBuf.encodeToByteArray<SyncPayloadSequence>(payload)
        if (bytes.isEmpty()) return

        val encodedString = encoder.encodeBytes(bytes)
        Logger.d(TAG) { "PREPARING PAYLOAD CHUNKS TOTAL PAYLOAD SIZE:${encodedString.length}" }

        withContext(Dispatchers.Default) {
            _lock.withLock {
                val chunks = encodedString.chunked(DEFAULT_WINDOW_SIZE)
                _dataQueue.addAll(chunks)
                Logger.d(TAG) { "PAYLOAD CHUNKS READY: COUNT:${chunks.size}" }
            }
        }
    }

    override suspend fun reset() {
        Logger.d(TAG) { "RESETTING THE SYNC OUT MANAGER STATE" }
        _lock.withLock {
            _dataQueue.clear()
        }
        _sequenceNumber.store(0)
    }

    companion object {
        private const val TAG = "SyncOutPayloadManager"
        private const val DEFAULT_WINDOW_SIZE = 16
        private const val MAX_READ_WITHOUT_ACK = 2
    }
}