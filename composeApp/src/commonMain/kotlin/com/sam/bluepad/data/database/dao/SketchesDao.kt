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
    fun readAllSketchesFlow(isDeleted: Boolean = false): Flow<List<SketchMetaDataAndContent>>

    @Transaction
    @Query("SELECT * FROM SKETCH_METADATA_TABLE WHERE (:includeDeleted = 1 OR is_deleted = 0)")
    suspend fun readAllSketches(includeDeleted: Boolean = true): List<SketchMetaDataAndContent>

    @Transaction
    @Query(
        """SELECT * FROM SKETCH_METADATA_TABLE
        WHERE _id IN (:ids)
        AND (:includeDeleted = 1 OR is_deleted = 0)
        """,
    )
    suspend fun readAllSketchesByIds(
        ids: List<Uuid>,
        includeDeleted: Boolean = true
    ): List<SketchMetaDataAndContent>

    @Transaction
    @Query("SELECT * FROM SKETCH_METADATA_TABLE WHERE is_deleted=:isDeleted LIMIT :limit OFFSET :offset")
    suspend fun readAllSketchesWithOffsetAndLimit(
        isDeleted: Boolean = false,
        offset: Int = 0,
        limit: Int = 10
    ): List<SketchMetaDataAndContent>

    // insert or delete values
    @Upsert
    suspend fun insertSketchContent(entity: SketchContentEntity)

    @Upsert
    suspend fun insertAuditLog(logs: SketchAuditLogEntity)

    @Upsert
    suspend fun insertSketchMetadata(metadata: SketchMetadataEntity)

    // content insert dao list version
    @Upsert
    suspend fun insertSketchContents(entities: List<SketchContentEntity>)
    @Upsert
    suspend fun insertAuditLogs(logs: List<SketchAuditLogEntity>)
    @Upsert
    suspend fun insertSketchMetadataList(metadata: List<SketchMetadataEntity>)

    @Transaction
    suspend fun upsertSketches(
        metadata: List<SketchMetadataEntity>,
        contents: List<SketchContentEntity>,
        logs: List<SketchAuditLogEntity>,
    ) {
        insertSketchMetadataList(metadata)
        insertSketchContents(contents)
        insertAuditLogs(logs)
    }

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
