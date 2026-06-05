package com.sam.bluepad.data.sync_diff

import com.sam.bluepad.data.sync_diff.dto.DiffChangesList
import com.sam.bluepad.data.sync_diff.mapper.toSyncChange
import com.sam.bluepad.domain.crypto.EncryptionSessionManager
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.domain.sync_diff.SyncDataSessionReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.Uuid

class SyncDataSessionReaderImpl private constructor(
    private val protoBuf: ProtoBuf,
    private val sessionManager: EncryptionSessionManager,
    private val timeZone: TimeZone,
) : SyncDataSessionReader {

    constructor(
        protoBuf: ProtoBuf,
        sessionManager: EncryptionSessionManager,
    ) : this(
        protoBuf = protoBuf,
        sessionManager = sessionManager,
        timeZone = TimeZone.currentSystemDefault(),
    )

    override suspend fun readSyncSession(session: Uuid): Result<List<SyncChanges>> {
        return try {
            val rawBytes = sessionManager.decryptAndReadData(session)
            val changesList = withContext(Dispatchers.Default) {
                protoBuf.decodeFromByteArray<DiffChangesList>(rawBytes)
            }
            val mappedResult = changesList.diffs.map { it.toSyncChange(timeZone) }

            Result.success(mappedResult)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        } finally {
            withContext(NonCancellable) {
                // it's a single read only content once read we delete the content
                sessionManager.deleteSessionData(session)
            }
        }
    }
}
