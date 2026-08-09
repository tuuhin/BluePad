package com.sam.bluepad.database.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.sam.bluepad.database.data.entities.SketchContentEntity
import kotlin.uuid.Uuid

@Dao
interface SketchContentDao {


    @Query("SELECT * FROM SKETCH_CONTENT_TABLE WHERE _id=:uuid")
    suspend fun getMetaDataId(uuid: Uuid): SketchContentEntity?
}
