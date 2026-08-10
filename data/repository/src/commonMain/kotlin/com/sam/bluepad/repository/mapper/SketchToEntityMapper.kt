package com.sam.bluepad.repository.mapper


import com.sam.bluepad.database.data.entities.SketchContentEntity
import com.sam.bluepad.database.data.entities.SketchMetadataEntity
import com.sam.bluepad.database.data.relations.SketchMetaDataAndContent
import com.sam.bluepad.model.sketches.CreateSketchModel
import com.sam.bluepad.model.sketches.SketchModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.Uuid

internal fun SketchModel.toMetaDataEntity(
    modifiedByDevice: Uuid,
    timezone: TimeZone = TimeZone.currentSystemDefault(),
): SketchMetadataEntity {
    return SketchMetadataEntity(
        id = id,
        title = title,
        createdAt = createdAt.toInstant(timezone),
        modifiedAt = modifiedAt.toInstant(timezone),
        version = version,
        isDeleted = isDeleted,
        createdByDeviceId = createdByDeviceId,
        lastModifiedByDevice = modifiedByDevice,
    )
}

internal fun SketchModel.toContent(
    contentHash: String,
    modifiedByDevice: Uuid,
    timezone: TimeZone = TimeZone.currentSystemDefault(),
): SketchContentEntity {
    return SketchContentEntity(
        id = id,
        content = content,
        contentHash = contentHash,
        modifiedAt = modifiedAt.toInstant(timezone),
        modifiedByDeviceId = modifiedByDevice,
    )
}

internal fun SketchMetaDataAndContent.toModel(timezone: TimeZone = TimeZone.currentSystemDefault()): SketchModel {
    return SketchModel(
        id = metaData.id,
        createdAt = metaData.createdAt.toLocalDateTime(timezone),
        modifiedAt = metaData.modifiedAt.toLocalDateTime(timezone),
        title = metaData.title,
        version = metaData.version,
        isDeleted = metaData.isDeleted,
        createdByDeviceId = metaData.createdByDeviceId,
        modifiedByDeviceId = metaData.lastModifiedByDevice,
        content = content.content,
        contentHash = content.contentHash,
    )
}

internal fun CreateSketchModel.toMetaDataEntity(
    timezone: TimeZone = TimeZone.currentSystemDefault(),
    deviceId: Uuid,
): SketchMetadataEntity {
    return SketchMetadataEntity(
        id = id,
        title = title,
        createdAt = createdAt.toInstant(timezone),
        modifiedAt = modifiedAt.toInstant(timezone),
        version = 1,
        isDeleted = false,
        createdByDeviceId = deviceId,
        lastModifiedByDevice = deviceId,
    )
}

internal fun CreateSketchModel.toContentEntity(
    timezone: TimeZone = TimeZone.currentSystemDefault(),
    deviceId: Uuid,
    contentHash: String,
): SketchContentEntity = SketchContentEntity(
    id = id,
    content = content,
    contentHash = contentHash,
    modifiedAt = modifiedAt.toInstant(timezone),
    modifiedByDeviceId = deviceId,
)
