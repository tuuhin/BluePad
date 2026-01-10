package com.sam.bluepad.domain.repository

import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

typealias FlowResourceSketches = Flow<Resource<SketchModel, Exception>>

interface SketchesRepository {

	fun getSketches(): Flow<Resource<List<SketchModel>, Exception>>
	fun getRevokedSketch(): Flow<Resource<List<SketchModel>, Exception>>

	fun getDeviceFromId(uuid: Uuid): FlowResourceSketches

	fun updateSketch(sketchModel: SketchModel, deviceId: Uuid): FlowResourceSketches
	fun createSketch(create: CreateSketchModel, deviceId: Uuid): FlowResourceSketches
	fun revokeSketch(sketchModel: SketchModel, deviceId: Uuid): Flow<Resource<Boolean, Exception>>
}