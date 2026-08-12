package com.sam.bluepad.data.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.sam.bluepad.data.database.entities.SketchAuditLogEntity
import com.sam.bluepad.data.database.entities.SketchMetadataEntity

data class SketchMetaDataAndChangelog(
    @Embedded val sketchMetaData: SketchMetadataEntity,
    @Relation(
        parentColumns = ["_id"],
        entityColumns = ["sketch_id"],
    ) val changelogs: List<SketchAuditLogEntity>
)
