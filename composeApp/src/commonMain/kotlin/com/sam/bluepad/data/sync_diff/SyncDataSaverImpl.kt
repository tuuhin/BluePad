package com.sam.bluepad.data.sync_diff

import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.domain.sync_diff.SyncDataSaver
import kotlin.uuid.Uuid

class SyncDataSaverImpl(
    private val repository: SketchesRepository,
    private val devicesRepo: ExternalDevicesRepository,
) : SyncDataSaver {

    override suspend fun submitSyncChanges(changes: List<SyncChanges>, externalDeviceId: Uuid): Result<Unit> =
        runCatching {

            // reads the external device from device id
            val device = devicesRepo.getDeviceByUuid(externalDeviceId)
                .getOrThrow()

            // we are concerned with only the indemnities that have changed or deleted (delete can be also consider a type of update)
            val changesIdentities = changes.map(SyncChanges::identity)
            val updatedOnes = repository.readSketchesByUUID(changesIdentities)
                .getOrThrow()

            // insert entries are absent on the device
            // these are being created by an external device only
            val newEntries = changes.filterIsInstance<SyncChanges.Insert>()
                .map {
                    SketchModel(
                        id = it.identity,
                        modifiedByDeviceId = device.id,
                        createdByDeviceId = device.id,
                        createdAt = it.incoming.modifiedAt,
                        modifiedAt = it.incoming.modifiedAt,
                        title = it.incoming.title,
                        content = it.incoming.content,
                        contentHash = it.incoming.contentHash,
                        isDeleted = it.incoming.isDeleted,
                    )
                }

            val deletedEntries = changes.filterIsInstance<SyncChanges.Delete>()
                .map {
                    val oldEntry = updatedOnes.find { dbEntry -> dbEntry.id == it.identity }
                    SketchModel(
                        id = it.identity,
                        modifiedByDeviceId = device.id,
                        createdByDeviceId = oldEntry?.createdByDeviceId ?: device.id,
                        createdAt = oldEntry?.createdAt ?: it.incoming.modifiedAt,
                        modifiedAt = it.incoming.modifiedAt,
                        // use the new content
                        title = it.incoming.title,
                        content = it.incoming.content,
                        contentHash = it.incoming.contentHash,
                        isDeleted = it.incoming.isDeleted,
                    )
                }

            val updatedEntries = changes.filterIsInstance<SyncChanges.Update>()
                .map {
                    val oldEntry = updatedOnes.find { dbEntry -> dbEntry.id == it.identity }
                    SketchModel(
                        id = it.identity,
                        modifiedByDeviceId = device.id,
                        createdByDeviceId = oldEntry?.createdByDeviceId ?: device.id,
                        createdAt = oldEntry?.createdAt ?: it.incoming.modifiedAt,
                        // update the new content
                        modifiedAt = it.incoming.modifiedAt,
                        title = it.incoming.title,
                        content = it.incoming.content,
                        contentHash = it.incoming.contentHash,
                        isDeleted = it.incoming.isDeleted,
                    )
                }

            val entriesToSave = (newEntries + deletedEntries + updatedEntries).distinctBy { it.id }

            // if the operation fails throw the error , error will propagate being wrapped with runCatching
            repository.upsertSketches(entriesToSave)
                .getOrThrow()
        }
}
