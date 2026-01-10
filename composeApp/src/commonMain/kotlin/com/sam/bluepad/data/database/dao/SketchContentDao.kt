package com.sam.bluepad.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.sam.bluepad.data.database.entities.SketchContentEntity
import kotlin.uuid.Uuid

@Dao
interface SketchContentDao {

	@Query("SELECT * FROM SKETCH_CONTENT_TABLE WHERE _id=:uuid")
	suspend fun getMetaDataId(uuid: Uuid): SketchContentEntity?
}