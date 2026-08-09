package com.sam.bluepad.database.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.sam.bluepad.database.data.entities.SketchAuditLogEntity
import com.sam.bluepad.database.data.entities.SketchMetadataEntity

internal data class SketchMetaDataAndChangelog(
    @Embedded val sketchMetaData: SketchMetadataEntity,
    @Relation(
        parentColumn = "_id",
        entityColumn = "sketch_id",
    ) val changelogs: List<SketchAuditLogEntity>
)
