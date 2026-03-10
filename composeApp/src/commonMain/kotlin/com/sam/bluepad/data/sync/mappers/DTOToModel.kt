package com.sam.bluepad.data.sync.mappers

import com.sam.bluepad.data.sync.dto.SyncDataFrame
import com.sam.bluepad.data.sync.dto.SyncPayloadSequence
import com.sam.bluepad.domain.sync.models.SyncContentDataModel
import com.sam.bluepad.domain.sync.models.SyncMetadataModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.Uuid

fun SyncPayloadSequence.MetaData.toSyncMetaDataList(timeZone: TimeZone = TimeZone.currentSystemDefault()): List<SyncMetadataModel> {
    return data.map { metadata ->
        SyncMetadataModel(
            contentHash = metadata.contentHash,
            title = metadata.title,
            itemId = metadata.itemId,
            version = metadata.version,
            lastModified = metadata.lastModified.toLocalDateTime(timeZone)
        )
    }
}

fun SyncPayloadSequence.Content.toSyncContent(timeZone: TimeZone = TimeZone.currentSystemDefault()): List<SyncContentDataModel> {
    return data.map { content ->
        SyncContentDataModel(content = content.content, itemId = content.itemId)
    }
}

fun List<SyncMetadataModel>.toPayloadSequence(timeZone: TimeZone = TimeZone.currentSystemDefault()): SyncPayloadSequence.MetaData {
    val data = map { metadata ->
        SyncDataFrame.Metadata(
            contentHash = metadata.contentHash,
            title = metadata.title,
            itemId = metadata.itemId,
            version = metadata.version,
            lastModified = metadata.lastModified.toInstant(timeZone)
        )
    }
    return SyncPayloadSequence.MetaData(data)
}


fun List<Uuid>.toPayloadSequence(): SyncPayloadSequence.ContentRequests {
    return SyncPayloadSequence.ContentRequests(data = map { SyncDataFrame.ContentRequest(it) })
}

fun List<SyncContentDataModel>.toPayloadSequence(timeZone: TimeZone = TimeZone.currentSystemDefault()): SyncPayloadSequence.Content {
    val data = map { content ->
        SyncDataFrame.Content(content = content.content, itemId = content.itemId)
    }
    return SyncPayloadSequence.Content(data)
}