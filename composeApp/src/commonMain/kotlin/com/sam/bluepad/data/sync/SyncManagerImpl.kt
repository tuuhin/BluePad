package com.sam.bluepad.data.sync

import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.SyncManager
import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.uuid.Uuid

private const val TAG = "SYNC_MANAGER_IMPL"

class SyncManagerImpl(
    private val repo: SketchesRepository,
) : SyncManager {

    override suspend fun findChangedItems(incoming: List<SyncMetadataModel>): Result<List<Uuid>> {
        val externalMetadata = incoming.map { it.itemId }
        val sketches =
            repo.readSketchesByUUID(externalMetadata).getOrElse { return Result.failure(it) }.associateBy { it.id }

        return coroutineScope {
            val finalList = incoming.asSequence().mapNotNull { metadata ->
                ensureActive()

                // if the item id is not found then it's a new item on the external device
                val model = sketches[metadata.itemId] ?: return@mapNotNull metadata.itemId

                // if any of version no ,content hash , modified at or title changed then
                // its a change in external device
                val changed =
                    metadata.version > model.version || metadata.contentHash != model.contentHash || metadata.lastModified > model.modifiedAt || metadata.title != model.title
                if (!changed) return@mapNotNull null
                metadata.itemId
            }.distinct().toList()

            Result.success(finalList)
        }
    }

    override suspend fun fetchContentForExchange(itemIds: List<Uuid>): Result<List<SyncContentDataModel>> {

        val sketches = repo.readSketchesByUUID(itemIds).getOrElse { return Result.failure(it) }

        val contentOut = sketches.map { SyncContentDataModel(content = it.content, itemId = it.id) }
        return Result.success(contentOut)
    }

    override suspend fun saveSyncContent(data: List<SyncContentDataModel>): Result<Unit> {
        Logger.d(TAG) { "SAVING NEW CONTENT TO DB" }
        Logger.d(TAG) { "DATA RECEIVED :$data" }
        // TODO FIX THIS LATER
        return Result.success(Unit)
    }

    override suspend fun getLocalSyncMetadata(): Result<List<SyncMetadataModel>> {
        val sketches = repo.readAllSketches().getOrElse { return Result.failure(it) }

        val contentOut = sketches.map {
            SyncMetadataModel(
                contentHash = it.contentHash,
                itemId = it.id,
                title = it.title,
                version = it.version,
                lastModified = it.modifiedAt,
            )
        }
        return Result.success(contentOut)
    }
}
