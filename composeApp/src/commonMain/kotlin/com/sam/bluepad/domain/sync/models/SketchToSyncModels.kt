package com.sam.bluepad.domain.sync.models

import com.sam.bluepad.domain.models.SketchModel

fun SketchModel.toMetadataModel(): SyncMetadataModel = SyncMetadataModel(
    contentHash = contentHash,
    title = title,
    itemId = id,
    lastModified = modifiedAt,
    version = version,
    isDeleted = isDeleted,
)

fun SketchModel.toContentModel(): SyncContentDataModel = SyncContentDataModel(
    itemId = id,
    modifiedAt = modifiedAt,
    title = title,
    content = content,
    contentHash = contentHash,
    version = version,
    isDeleted = isDeleted,
    modifiedByDevice = modifiedByDeviceId,
    createdByDevice = createdByDeviceId,
)
