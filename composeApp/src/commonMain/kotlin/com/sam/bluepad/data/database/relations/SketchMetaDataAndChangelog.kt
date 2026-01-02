package com.sam.bluepad.data.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.sam.bluepad.data.database.entities.SketchMetadataEntity
import com.sam.bluepad.data.database.entities.SketchUpdateLogEntity

data class SketchMetaDataAndChangelog(
	@Embedded val sketchMetaData: SketchMetadataEntity,
	@Relation(
		parentColumn = "_id",
		entityColumn = "sketch_id"
	) val changelogs: List<SketchUpdateLogEntity>
)