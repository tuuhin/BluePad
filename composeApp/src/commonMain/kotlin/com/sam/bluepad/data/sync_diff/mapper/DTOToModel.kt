package com.sam.bluepad.data.sync_diff.mapper

import com.sam.bluepad.data.sync_diff.dto.SyncDiffChangesDTO
import com.sam.bluepad.data.sync_diff.dto.SyncSnapshotDto
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.domain.sync_diff.SyncSnapshotModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private fun SyncSnapshotDto.toModel(timeZone: TimeZone = TimeZone.currentSystemDefault()) = SyncSnapshotModel(
    id = id,
    version = version,
    title = title,
    content = content,
    contentHash = contentHash,
    modifiedAt = modifiedAt.toLocalDateTime(timeZone),
    isDeleted = isDeleted,
)

private fun SyncSnapshotModel.toDTO(timeZone: TimeZone = TimeZone.currentSystemDefault()) = SyncSnapshotDto(
    id = id,
    version = version,
    title = title,
    content = content,
    contentHash = contentHash,
    modifiedAt = modifiedAt.toInstant(timeZone),
    isDeleted = isDeleted,
)

fun SyncDiffChangesDTO.toSyncChange(timeZone: TimeZone = TimeZone.currentSystemDefault()) = when (this) {
    is SyncDiffChangesDTO.DeleteChange -> SyncChanges.Delete(
        local = local.toModel(timeZone),
        incoming = incoming.toModel(timeZone),
    )

    is SyncDiffChangesDTO.InsertChange -> SyncChanges.Insert(incoming = incoming.toModel(timeZone))

    is SyncDiffChangesDTO.UpdateChange -> SyncChanges.Update(
        local = local.toModel(timeZone),
        incoming = incoming.toModel(timeZone),
    )

    is SyncDiffChangesDTO.ConflictChange -> SyncChanges.Conflict(
        local = local.toModel(timeZone),
        incoming = incoming.toModel(timeZone),
    )
}


fun SyncChanges.toDTO(timeZone: TimeZone = TimeZone.currentSystemDefault()) = when (this) {
    is SyncChanges.Insert -> SyncDiffChangesDTO.InsertChange(incoming = incoming.toDTO(timeZone))
    is SyncChanges.Delete -> SyncDiffChangesDTO.DeleteChange(
        local = local.toDTO(timeZone),
        incoming = incoming.toDTO(timeZone),
    )

    is SyncChanges.Update -> SyncDiffChangesDTO.UpdateChange(
        local = local.toDTO(timeZone),
        incoming = incoming.toDTO(timeZone),
    )

    is SyncChanges.Conflict -> SyncDiffChangesDTO.ConflictChange(
        local = local.toDTO(timeZone),
        incoming = incoming.toDTO(timeZone),
    )
}
