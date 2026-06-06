package com.sam.bluepad.domain.repository

import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

typealias FlowResourceSketches = Flow<Resource<SketchModel, Exception>>
typealias Sketches = List<SketchModel>

/**
 * Repository interface for managing sketches and their synchronization state.
 */
interface SketchesRepository {

    /**
     * Returns a flow of all active (non-deleted) sketches.
     */
    fun getSketches(): Flow<Resource<List<SketchModel>, Exception>>

    /**
     * Returns a flow of all revoked (deleted) sketches.
     */
    fun getRevokedSketch(): Flow<Resource<List<SketchModel>, Exception>>

    /**
     * Returns a flow containing a single sketch identified by its [uuid].
     */
    fun getSketchFromIdFlow(uuid: Uuid): FlowResourceSketches

    /**
     * Updates an existing sketch in the repository.
     * @param sketchModel The updated sketch data.
     * @param deviceId The ID of the device performing the update.
     */
    fun updateSketch(sketchModel: SketchModel, deviceId: Uuid): FlowResourceSketches

    /**
     * Creates a new sketch in the repository.
     * @param create The data for the new sketch.
     * @param deviceId The ID of the device creating the sketch.
     */
    fun createSketch(create: CreateSketchModel, deviceId: Uuid): FlowResourceSketches

    /**
     * Marks a sketch as revoked (deleted).
     * @param sketchModel The sketch to revoke.
     * @param deviceId The ID of the device performing the revocation.
     */
    fun revokeSketch(sketchModel: SketchModel, deviceId: Uuid): Flow<Resource<Boolean, Exception>>

    /**
     * Reads a paginated list of sketches.
     * @param offset The starting position.
     * @param count The number of items to read.
     */
    suspend fun readSketches(offset: Int = 0, count: Int = 10): Result<List<SketchModel>>

    /**
     * Reads all sketches from the repository.
     */
    suspend fun readAllSketches(): Result<Sketches>

    /**
     * Reads a list of sketches identified by their [uuids].
     */
    suspend fun readSketchesByUUID(uuids: List<Uuid>): Result<List<SketchModel>>

    /**
     * Performs a bulk upsert (insert or update) of multiple sketches.
     * Used primarily during synchronization to apply incoming changes.
     */
    suspend fun upsertSketches(sketches: List<SketchModel>): Result<Unit>
}
