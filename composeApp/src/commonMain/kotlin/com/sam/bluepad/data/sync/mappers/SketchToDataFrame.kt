package com.sam.bluepad.data.sync.mappers

import com.sam.bluepad.data.sync.dto.SyncDataFrame
import com.sam.bluepad.domain.models.SketchModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

fun SketchModel.toSyncMetadataFrame(timezone: TimeZone = TimeZone.currentSystemDefault()): SyncDataFrame.Metadata {
    return SyncDataFrame.Metadata(
        contentHash = contentHash,
        title = title,
        itemId = id,
        lastModified = modifiedAt.toInstant(timezone),
        version = version
    )
}

fun SketchModel.toSyncContentFrame(timezone: TimeZone = TimeZone.currentSystemDefault()): SyncDataFrame.Content {
    return SyncDataFrame.Content(content = content, itemId = id)
}