package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

private const val TAG = "SYNC_MANAGER_IMPL"

class SyncManagerImpl(
    private val repo: SketchesRepository,
) : SyncManager {

    override suspend fun computeUpdatedOrNewItems(metadata: List<SyncMetadataModel>): Result<List<Uuid>> {
        return withContext(Dispatchers.Default) {
            try {
                val externalMetadata = metadata.map { it.itemId }
                val sketches = repo.readSketchesByUUID(externalMetadata)
                    .getOrThrow()

                val sketchesMap = sketches.associateBy { it.id }

                val finalList = metadata.asSequence().mapNotNull { metadata ->
                    ensureActive()

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
    }

    override suspend fun performSyncResultsOperation(results: List<SyncContentDataModel>): Result<Unit> {
        Logger.i(tag = TAG) { "=========================== FINAL DATA RECEIVED :$results" }
        return runCatching {
            Logger.d(tag = TAG) { "SAVING NEW CONTENT TO DB" }
//                repo.updateSketches()
        }
    }


    private fun SyncMetadataModel.hasContentChange(model: SketchModel) =
        version > model.version ||
            contentHash != model.contentHash ||
            lastModified > model.modifiedAt ||
            title != model.title
}
