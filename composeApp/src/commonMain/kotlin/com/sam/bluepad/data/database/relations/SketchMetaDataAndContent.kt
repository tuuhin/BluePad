package com.sam.bluepad.data.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.sam.bluepad.data.database.entities.SketchContentEntity
import com.sam.bluepad.data.database.entities.SketchMetadataEntity

data class SketchMetaDataAndContent(
	@Embedded val metaData: SketchMetadataEntity,
	@Relation(
		parentColumn = "_id",
		entityColumn = "_id"
	)
	val content: SketchContentEntity
)