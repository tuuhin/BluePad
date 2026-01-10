package com.sam.bluepad.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.sam.bluepad.data.database.entities.SketchMetadataEntity
import kotlin.uuid.Uuid

@Dao
interface SketchMetadataDao {

	@Query("SELECT * FROM SKETCH_METADATA_TABLE WHERE _id=:uuid")
	suspend fun getMetaDataId(uuid: Uuid): SketchMetadataEntity?
}