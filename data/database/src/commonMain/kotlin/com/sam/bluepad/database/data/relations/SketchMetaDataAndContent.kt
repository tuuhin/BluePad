package com.sam.bluepad.database.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.sam.bluepad.database.data.entities.SketchContentEntity
import com.sam.bluepad.database.data.entities.SketchMetadataEntity

data class SketchMetaDataAndContent(
    @Embedded val metaData: SketchMetadataEntity,
    @Relation(
        parentColumn = "_id",
        entityColumn = "_id",
    )
    val content: SketchContentEntity
)
