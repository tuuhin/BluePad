package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.data.sync.mappers.toPayloadSequence
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.compression.ICompressionManager
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.settings.SyncSettingsProvider
import com.sam.bluepad.domain.sync.OutPayloadManager
import com.sam.bluepad.domain.sync.exceptions.OutgoingDataException
import com.sam.bluepad.domain.sync.models.FragmentedDataBlock
import com.sam.bluepad.domain.sync.models.SyncDataPayload
import com.sam.bluepad.domain.sync.models.toMetadataModel
import com.sam.bluepad.domain.use_cases.BytesEncoder
import kotlinx.coroutines.NonCancellable
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
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.uuid.Uuid

private const val TAG = "SYNC_OUT_PAYLOAD_MANAGER"

@OptIn(ExperimentalAtomicApi::class)
class OutgoingPayloadManagerImpl private constructor(
    private val protoBuf: ProtoBuf,
    private val encoder: BytesEncoder,
    private val sketchRepository: SketchesRepository,
    private val timezone: TimeZone,
    private val compressor: ICompressionManager,
    private val dispatchers: PlatformDispatcherProvider,
    private val syncSettings: SyncSettingsProvider,
) : OutPayloadManager {

    constructor(
        protoBuf: ProtoBuf,
        encoder: BytesEncoder,
        repo: SketchesRepository,
        dispatchers: PlatformDispatcherProvider,
        compressor: ICompressionManager,
        syncSettings: SyncSettingsProvider
    ) : this(
        protoBuf = protoBuf,
        encoder = encoder,
        compressor = compressor,
        sketchRepository = repo,
        dispatchers = dispatchers,
        syncSettings = syncSettings,
        timezone = TimeZone.currentSystemDefault(),
    )


    private val _lock = Mutex()
    private val _sessions = HashMap<Uuid, SessionState>()

    override suspend fun prepareChunks(sessionId: Uuid, type: SyncDataPayload.Outgoing): Result<Unit> {
        val session = _lock.withLock {
            _sessions.computeIfAbsent(sessionId) { SessionState() }
        }

        // Reset state if session is being reused
        session.lock.withLock {
            session.dataQueue.clear()
            session.ackMap.clear()
            session.sequenceNumber.store(0)
            session.readSinceLastAck.store(0)
        }

        Logger.d(tag = TAG) { "CREATING CHUNKS SET FOR SESSION:$sessionId TYPE:$type ${sketchRepository.javaClass}" }

        val payloadSeq = when (type) {
            SyncDataPayload.Metadata ->
                sketchRepository.readAllSketches()
                    .getOrElse { err -> return Result.failure(err) }
                    .map(SketchModel::toMetadataModel)
                    .toPayloadSequence(timezone)

            is SyncDataPayload.ContentIdsQuery -> type.ids.toPayloadSequence()
            is SyncDataPayload.ContentPayload -> type.contentData.toPayloadSequence(timezone)
        }

        return runCatching { chunkAndSequencePayload(session, payloadSeq) }
    }


    override suspend fun getHasMoreChunks(sessionId: Uuid): Boolean {
        val session = _lock.withLock { _sessions[sessionId] ?: return false }
        return session.lock.withLock {
            session.dataQueue.isNotEmpty()
        }
    }

    override suspend fun getNextChunk(sessionId: Uuid): Result<FragmentedDataBlock> = runCatching {
        val session = _lock.withLock {
            _sessions[sessionId] ?: throw OutgoingDataException.SessionNotFoundException(sessionId)
        }

        // An acknowledgement is required to continue
        if (session.readSinceLastAck.load() >= MAX_READ_WITHOUT_ACK) throw OutgoingDataException.WindowLimitReachedException()


        val poll = session.lock.withLock {
            session.dataQueue.poll() ?: throw OutgoingDataException.NoMoreChunksException()
        }

        val nextSeqNumber = session.sequenceNumber.incrementAndFetch()
            .toUInt()
        session.readSinceLastAck.incrementAndFetch()

        session.ackMap[nextSeqNumber] = false
        FragmentedDataBlock(seqNumber = nextSeqNumber, payload = poll)
    }

    override suspend fun markChunkAck(sessionId: Uuid, seq: UInt) {
        val session = _lock.withLock { _sessions[sessionId] ?: return }
        session.lock.withLock {
            if (session.ackMap.containsKey(seq)) {
                session.ackMap[seq] = true
                session.readSinceLastAck.decrementAndFetch()
            }
        }
    }


    private suspend fun chunkAndSequencePayload(session: SessionState, payload: SyncPayloadSequence) {
        val bytes = withContext(dispatchers.io) { protoBuf.encodeToByteArray<SyncPayloadSequence>(payload) }

        val settings = syncSettings.settings()
        val level = settings.syncCompressionLevel
        // a time check for
        val compressedBytes = withContext(dispatchers.default) {
            compressor.compressBytes(bytes, level)
        }

        Logger.d(tag = TAG) { "TOTAL BYTES TO BE SEND UN-COMPRESSED :${bytes.size} COMPRESSED:${compressedBytes.size}" }

        if (compressedBytes.isEmpty()) return

        val encodedString = encoder.encodeBytes(compressedBytes)
        Logger.d(tag = TAG) { "PREPARING PAYLOAD TOTAL SIZE:${encodedString.length}" }

        val windowSize = syncSettings.settings()
            .syncPayloadSize.coerceIn(0..MAX_WINDOW_SIZE)

        session.lock.withLock {
            val chunks = encodedString.chunked(windowSize)
            session.dataQueue.addAll(chunks)
            Logger.d(tag = TAG) { "PAYLOAD CHUNKS READY NUMBER OF BLOCKS:${chunks.size}" }
        }

    }

    override suspend fun reset() {
        Logger.d(tag = TAG) { "RESETTING ALL SYNC OUT MANAGER SESSIONS" }
        withContext(NonCancellable) {
            _sessions.clear()
        }
    }

    private class SessionState {
        val lock = Mutex()
        val dataQueue = ConcurrentLinkedQueue<String>()
        val ackMap = ConcurrentHashMap<UInt, Boolean>()
        val sequenceNumber = AtomicInt(0)
        val readSinceLastAck = AtomicInt(0)
    }


    companion object {
        private const val MAX_WINDOW_SIZE = 480
        private const val MAX_READ_WITHOUT_ACK = 2
    }
}
