package com.sam.bluepad.data.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.sam.bluepad.data.database.entities.SketchContentEntity
import com.sam.bluepad.data.database.entities.SketchMetadataEntity

data class SketchMetaDataAndContent(
    @Embedded val metaData: SketchMetadataEntity,
    @Relation(
        parentColumns = ["_id"],
        entityColumns = ["_id"],
    )
    val content: SketchContentEntity
)
