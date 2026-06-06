package com.sam.bluepad.data.repository

import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.FlowResourceSketches
import com.sam.bluepad.domain.repository.Sketches
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

class FakeSketchesRepoImpl : SketchesRepository {

    private val _sketches = mutableListOf<SketchModel>()

    override fun getSketches(): Flow<Resource<List<SketchModel>, Exception>> =
        flow {
            emit(Resource.Loading)

            emit(Resource.Success(_sketches.filter { !it.isDeleted }))
        }

    override fun getRevokedSketch(): Flow<Resource<List<SketchModel>, Exception>> =
        flow {
            emit(Resource.Loading)
            emit(Resource.Success(_sketches.filter { it.isDeleted }))
        }

    override fun getSketchFromIdFlow(uuid: Uuid): FlowResourceSketches = flow {
        emit(Resource.Loading)
        val sketch = _sketches.find { it.id == uuid }
        if (sketch != null) {
            emit(Resource.Success(sketch))
        } else {
            emit(Resource.Error(Exception("Sketch not found")))
        }
    }

    override fun updateSketch(
        sketchModel: SketchModel,
        deviceId: Uuid
    ): FlowResourceSketches = flow {
        emit(Resource.Loading)
        val index = _sketches.indexOfFirst { it.id == sketchModel.id }
        if (index != -1) {
            val updated = sketchModel.copy(
                modifiedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                modifiedByDeviceId = deviceId
            )
            _sketches[index] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error(Exception("Sketch not found")))
        }
    }

    override fun createSketch(
        create: CreateSketchModel,
        deviceId: Uuid
    ): FlowResourceSketches = flow {
        emit(Resource.Loading)
        val newSketch = SketchModel(
            id = create.id,
            title = create.title,
            content = create.content,
            createdAt = create.createdAt,
            modifiedAt = create.modifiedAt,
            createdByDeviceId = deviceId,
            modifiedByDeviceId = deviceId
        )
        _sketches.add(newSketch)
        emit(Resource.Success(newSketch))
    }

    override fun revokeSketch(
        sketchModel: SketchModel,
        deviceId: Uuid
    ): Flow<Resource<Boolean, Exception>> = flow {
        emit(Resource.Loading)
        val index = _sketches.indexOfFirst { it.id == sketchModel.id }
        if (index != -1) {
            _sketches[index] = _sketches[index].copy(
                isDeleted = true,
                modifiedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                modifiedByDeviceId = deviceId
            )
            emit(Resource.Success(true))
        } else {
            emit(Resource.Error(Exception("Sketch not found")))
        }
    }

    override suspend fun readSketches(offset: Int, count: Int): Result<List<SketchModel>> {
        return runCatching { _sketches.drop(offset).take(count) }
    }

    override suspend fun readAllSketches(): Result<Sketches> {
        return Result.success(_sketches.toList())
    }

    override suspend fun readSketchesByUUID(uuids: List<Uuid>): Result<List<SketchModel>> {
        val filtered = _sketches.filter { it.id in uuids }
        return Result.success(filtered)
    }

    fun createSketchForTest(model: CreateSketchModel, deviceId: Uuid) {
        val data = SketchModel(
            id = model.id,
            title = model.title,
            content = model.content,
            createdAt = model.createdAt,
            modifiedAt = model.modifiedAt,
            createdByDeviceId = deviceId,
            modifiedByDeviceId = deviceId
        )
        _sketches.add(data)
    }

    override suspend fun upsertSketches(sketches: List<SketchModel>): Result<Unit> {
        sketches.forEach { incoming ->
            val index = this._sketches.indexOfFirst { it.id == incoming.id }
            if (index != -1)
                this._sketches[index] = incoming
             else
                this._sketches.add(incoming)

        }
        return Result.success(Unit)
    }
}
