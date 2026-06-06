package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.sync_diff.dto.DiffChangesList
import com.sam.bluepad.data.sync_diff.mapper.toDTO
import com.sam.bluepad.domain.crypto.EncryptionSessionManager
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import com.sam.bluepad.domain.sync_diff.SyncDiffCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.Uuid

private const val TAG = "SYNC_MANAGER_IMPL"

class SyncManagerImpl(
    private val repo: SketchesRepository,
    private val diffManagerImpl: SyncDiffCalculator,
    private val protoBuf: ProtoBuf,
    private val encryptedSessionManger: EncryptionSessionManager,
) : SyncManager {

    override suspend fun readChangedItemsIds(metadata: List<SyncMetadataModel>): Result<List<Uuid>> {
        return try {
            val externalMetadata = metadata.map { it.itemId }
            val sketches = repo.readSketchesByUUID(externalMetadata)
                .getOrThrow()

            val sketchesMap = sketches.associateBy { it.id }

            val finalList = metadata.asSequence().mapNotNull { metadata ->
                // if the item id is not found then it's a new item on the external device
                val model = sketchesMap[metadata.itemId] ?: return@mapNotNull metadata.itemId

                val hasChanged = metadata.hasContentChange(model)
                if (!hasChanged) return@mapNotNull null
                metadata.itemId

            }.distinct().toList()
            Result.success(finalList)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }

    }

    override suspend fun performSyncResultsOperation(
        sessionId: Uuid,
        results: List<SyncContentDataModel>
    ): Result<Unit> {
        return runCatching {
            Logger.d(tag = TAG) { "SAVING REMOTE DATA CONTENT AS CACHE" }

            val syncChanges = diffManagerImpl.computeDiff(results)
                .getOrElse { err -> return Result.failure(err) }

            val dto = DiffChangesList(diffs = syncChanges.map { it.toDTO() }.toSet())
            val bytes = protoBuf.encodeToByteArray<DiffChangesList>(dto)
            encryptedSessionManger.encryptDataAndSave(sessionId, bytes)
        }
    }

    private fun SyncMetadataModel.hasContentChange(model: SketchModel) =
        version != model.version ||
            contentHash != model.contentHash ||
            lastModified != model.modifiedAt ||
            isDeleted != model.isDeleted ||
            title != model.title
}
