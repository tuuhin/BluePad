package com.sam.bluepad.domain.repository

import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

typealias FlowResourceSketches = Flow<Resource<SketchModel, Exception>>
typealias Sketches = List<SketchModel>

interface SketchesRepository {

    fun getSketches(): Flow<Resource<List<SketchModel>, Exception>>
    fun getRevokedSketch(): Flow<Resource<List<SketchModel>, Exception>>

    fun getDeviceFromId(uuid: Uuid): FlowResourceSketches

    fun updateSketch(sketchModel: SketchModel, deviceId: Uuid): FlowResourceSketches
    fun createSketch(create: CreateSketchModel, deviceId: Uuid): FlowResourceSketches
    fun revokeSketch(sketchModel: SketchModel, deviceId: Uuid): Flow<Resource<Boolean, Exception>>

    suspend fun readSketches(offset: Int = 0, count: Int = 10): Result<List<SketchModel>>
    suspend fun readAllSketches(): Result<Sketches>

    suspend fun readSketchesByUUID(uuids: List<Uuid>): Result<List<SketchModel>>
}