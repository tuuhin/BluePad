package com.sam.bluepad.data.sync_diff

import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.domain.sync_diff.SyncDiffCalculator
import com.sam.bluepad.domain.sync_diff.SyncSnapshotModel

class SyncDiffCalculatorImpl(
    private val repository: SketchesRepository
) : SyncDiffCalculator {

    override suspend fun computeDiff(changes: List<SyncContentDataModel>): Result<Set<SyncChanges>> {

        val entries = mutableSetOf<SyncChanges>()

        val localSketches = repository.readAllSketches()
            .getOrElse { err -> return Result.failure(err) }
            .associateBy { it.id }

        val remoteSketches = changes.associateBy { it.itemId }

        val allIds = localSketches.keys + remoteSketches.keys

        for (id in allIds) {

            val local = localSketches[id]
            val remote = remoteSketches[id]

            when {
                local == null && remote != null -> entries.add(SyncChanges.Insert(incoming = remote.toSnapshot()))
                local != null && remote == null -> continue
                local != null && remote != null -> {

                    val localSnapshot = local.toSnapshot()
                    val remoteSnapshot = remote.toSnapshot()

                    // Delete if remote is deleted and remote version is higher
                    if (remote.isDeleted && remote.version > local.version) {
                        entries.add(SyncChanges.Delete(local = localSnapshot, incoming = remoteSnapshot))
                        continue
                    }
                    // Update if remote version is higher
                    if (remote.version > local.version) {
                        entries.add(SyncChanges.Update(local = localSnapshot, incoming = remoteSnapshot))
                        continue
                    }

                    val isNotSame = remote.contentHash != local.contentHash ||
                        remote.title != local.title ||
                        remote.isDeleted != local.isDeleted

                    // if versions are same but the content data is not matching
                    if (remote.version == local.version && isNotSame) {
                        entries.add(SyncChanges.Conflict(local = localSnapshot, incoming = remoteSnapshot))
                        continue
                    }
                }
            }
        }
        return Result.success(entries)
    }

    private fun SketchModel.toSnapshot() = SyncSnapshotModel(
        id = id,
        version = version,
        title = title,
        content = content,
        contentHash = contentHash,
        modifiedAt = modifiedAt,
        isDeleted = isDeleted,
    )

    private fun SyncContentDataModel.toSnapshot() = SyncSnapshotModel(
        id = itemId,
        version = version,
        title = title,
        content = content,
        contentHash = contentHash,
        modifiedAt = modifiedAt,
        isDeleted = isDeleted,
    )
}
