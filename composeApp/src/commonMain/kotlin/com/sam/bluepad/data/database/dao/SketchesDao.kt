package com.sam.bluepad.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sam.bluepad.data.database.entities.SketchAuditLogEntity
import com.sam.bluepad.data.database.entities.SketchContentEntity
import com.sam.bluepad.data.database.entities.SketchMetadataEntity
import com.sam.bluepad.data.database.relations.SketchMetaDataAndContent
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface SketchesDao {

	@Transaction
	@Query("SELECT * FROM SKETCH_METADATA_TABLE WHERE _id=:uuid")
	suspend fun getSketchFromId(uuid: Uuid): SketchMetaDataAndContent?

	@Transaction
	@Query("SELECT * FROM SKETCH_METADATA_TABLE WHERE is_deleted=:isDeleted")
	fun getAllSketches(isDeleted: Boolean = false): Flow<List<SketchMetaDataAndContent>>

	// insert or delete values
	@Upsert
	suspend fun insertSketchContent(entity: SketchContentEntity)

	@Upsert
	suspend fun insertAuditLog(logs: SketchAuditLogEntity)

	@Upsert
	suspend fun insertSketchMetadata(metadata: SketchMetadataEntity)

	@Transaction
	suspend fun insertSketchMetaDataAndContent(
		metadata: SketchMetadataEntity,
		content: SketchContentEntity,
		logs: SketchAuditLogEntity,
	): Uuid {
		insertSketchMetadata(metadata)
		insertSketchContent(content)
		insertAuditLog(logs)
		return metadata.id
	}

}