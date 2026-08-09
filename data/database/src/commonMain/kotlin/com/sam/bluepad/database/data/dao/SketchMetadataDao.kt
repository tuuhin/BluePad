package com.sam.bluepad.database.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.sam.bluepad.database.data.entities.SketchMetadataEntity
import kotlin.uuid.Uuid

@Dao
interface SketchMetadataDao {


    @Query("SELECT * FROM SKETCH_METADATA_TABLE WHERE _id=:uuid")
    suspend fun getMetaDataId(uuid: Uuid): SketchMetadataEntity?
}
