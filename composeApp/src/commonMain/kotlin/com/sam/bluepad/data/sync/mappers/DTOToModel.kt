package com.sam.bluepad.data.sync.mappers

import com.sam.bluepad.data.sync.dto.SyncDataFrame
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.Uuid


fun SyncPayloadSequence.MetaData.toSyncMetadataList(timeZone: TimeZone) = data.map { payload ->
    SyncMetadataModel(
        contentHash = payload.contentHash,
        title = payload.title,
        itemId = payload.itemId,
        version = payload.version,
        lastModified = payload.lastModified.toLocalDateTime(timeZone),
    )
}

fun List<SyncMetadataModel>.toPayloadSequence(timeZone: TimeZone) = SyncPayloadSequence.MetaData(
    data = map { metadata ->
        SyncDataFrame.Metadata(
            contentHash = metadata.contentHash,
            title = metadata.title,
            itemId = metadata.itemId,
            version = metadata.version,
            lastModified = metadata.lastModified.toInstant(timeZone),
        )
    },
)

fun List<Uuid>.toPayloadSequence() =
    SyncPayloadSequence.ContentRequests(data = map { SyncDataFrame.ContentRequest(it) })


fun SyncPayloadSequence.Content.toContentList(timeZone: TimeZone) = data.map { content ->
    SyncContentDataModel(
        itemId = content.itemId,
        modifiedAt = content.modifiedAt.toLocalDateTime(timeZone),
        title = content.title,
        content = content.content,
        contentHash = content.contentHash,
        version = content.version,
        isDeleted = content.isDeleted,
        modifiedByDevice = content.modifiedByDevice,
        createdByDevice = content.createdByDevice,
    )
}

fun List<SyncContentDataModel>.toPayloadSequence(timeZone: TimeZone = TimeZone.currentSystemDefault()): SyncPayloadSequence.Content {
    val data = map { content ->
        SyncDataFrame.Content(
            itemId = content.itemId,
            modifiedAt = content.modifiedAt.toInstant(timeZone),
            title = content.title,
            content = content.content,
            contentHash = content.contentHash,
            version = content.version,
            isDeleted = content.isDeleted,
            modifiedByDevice = content.modifiedByDevice,
            createdByDevice = content.createdByDevice,
        )
    }
    return SyncPayloadSequence.Content(data)
}
