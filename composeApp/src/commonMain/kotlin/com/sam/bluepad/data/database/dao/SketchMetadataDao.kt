package com.sam.bluepad.data.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.sam.bluepad.data.database.entities.SketchMetadataEntity
import kotlin.uuid.Uuid

@Dao
interface SketchMetadataDao {

	@Query("SELECT * FROM SKETCH_METADATA_TABLE WHERE _id=:uuid")
	suspend fun getMetaDataId(uuid: Uuid): SketchMetadataEntity?
}
