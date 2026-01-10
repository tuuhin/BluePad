package com.sam.bluepad.data.repository

import com.sam.bluepad.data.database.dao.SketchContentDao
import com.sam.bluepad.data.database.dao.SketchMetadataDao
import com.sam.bluepad.data.database.dao.SketchesDao
import com.sam.bluepad.data.database.entities.SketchAuditLogEntity
import com.sam.bluepad.data.database.relations.SketchMetaDataAndContent
import com.sam.bluepad.data.mappers.toContent
import com.sam.bluepad.data.mappers.toContentEntity
import com.sam.bluepad.data.mappers.toMetaDataEntity
import com.sam.bluepad.data.mappers.toModel
import com.sam.bluepad.domain.exceptions.InvalidSketchIdException
import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.models.SketchChangeType
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.FlowResourceSketches
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.use_cases.HashGenerator
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.domain.utils.handleDBOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.uuid.Uuid

class SketchesRepoImpl(
	private val sketchesDao: SketchesDao,
	private val sketchMetadataDao: SketchMetadataDao,
	private val sketchContentDao: SketchContentDao,
	private val hasher: HashGenerator,
) : SketchesRepository {

	private val timeZone = TimeZone.currentSystemDefault()

	override fun getRevokedSketch(): Flow<Resource<List<SketchModel>, Exception>> {
		return sketchesDao.getAllSketches(true)
			.map<List<SketchMetaDataAndContent>, Resource<List<SketchModel>, Exception>> { entities ->
				val sketches = entities.map { it.toModel(timezone = timeZone) }
				Resource.Success(sketches)
			}
			.onStart { emit(Resource.Loading) }
			.catch { err ->
				if (err is Exception) emit(Resource.Error(err))
			}
	}

	override fun getSketches(): Flow<Resource<List<SketchModel>, Exception>> {
		return sketchesDao.getAllSketches(false)
			.map<List<SketchMetaDataAndContent>, Resource<List<SketchModel>, Exception>> { entities ->
				val sketches = entities.map { it.toModel(timezone = timeZone) }
				Resource.Success(sketches)
			}
			.onStart { emit(Resource.Loading) }
			.catch { err ->
				if (err is Exception)
					emit(Resource.Error(err))
			}
	}

	override fun getDeviceFromId(uuid: Uuid): Flow<Resource<SketchModel, Exception>> {
		return handleDBOperation {
			val sketchContent = sketchesDao.getSketchFromId(uuid) ?: run {
				return@handleDBOperation Resource.Error(InvalidSketchIdException())
			}
			val sketchModel = sketchContent.toModel(timeZone)
			Resource.Success(sketchModel)
		}
	}

	override fun createSketch(create: CreateSketchModel, deviceId: Uuid): FlowResourceSketches {
		return handleDBOperation {

			val metaData = create.toMetaDataEntity(timeZone, deviceId = deviceId)
			val contentHash = hasher.generateHash(create.content)
			val content = create.toContentEntity(timeZone, deviceId = deviceId, contentHash)

			val log = SketchAuditLogEntity(
				id = Uuid.random(),
				sketchId = create.id,
				changeType = SketchChangeType.CREATE,
				prevVersion = 0,
				newVersion = 1,
				modifiedAt = metaData.modifiedAt,
				deviceId = deviceId
			)

			// finally save the data
			val uuid = sketchesDao.insertSketchMetaDataAndContent(metaData, content, log)
			val sketchContent = sketchesDao.getSketchFromId(uuid) ?: run {
				return@handleDBOperation Resource.Error(InvalidSketchIdException())
			}
			val sketchModel = sketchContent.toModel(timeZone)
			Resource.Success(sketchModel)
		}
	}

	override fun updateSketch(sketchModel: SketchModel, deviceId: Uuid): FlowResourceSketches {
		return handleDBOperation {
			val result = sketchMetadataDao.getMetaDataId(sketchModel.id) ?: run {
				return@handleDBOperation Resource.Error(InvalidSketchIdException())
			}

			val metaData = sketchModel.toMetaDataEntity(deviceId, timeZone)
				.copy(modifiedAt = Clock.System.now())
			val contentHash = hasher.generateHash(sketchModel.content)


			val content = sketchModel.toContent(contentHash, deviceId, timeZone)
				.copy(modifiedAt = Clock.System.now())

			val log = SketchAuditLogEntity(
				id = Uuid.random(),
				sketchId = result.id,
				changeType = SketchChangeType.UPDATE,
				prevVersion = result.version,
				newVersion = result.version + 1,
				modifiedAt = metaData.modifiedAt,
				deviceId = deviceId
			)

			val uuid = sketchesDao.insertSketchMetaDataAndContent(metaData, content, log)
			val sketchContent = sketchesDao.getSketchFromId(uuid) ?: run {
				return@handleDBOperation Resource.Error(InvalidSketchIdException())
			}
			val sketchModel = sketchContent.toModel(timeZone)
			Resource.Success(sketchModel)
		}
	}

	override fun revokeSketch(
		sketchModel: SketchModel,
		deviceId: Uuid
	): Flow<Resource<Boolean, Exception>> {
		return handleDBOperation {
			val result = sketchMetadataDao.getMetaDataId(sketchModel.id) ?: run {
				return@handleDBOperation Resource.Error(InvalidSketchIdException())
			}

			val sketchContent = sketchContentDao.getMetaDataId(sketchModel.id) ?: run {
				return@handleDBOperation Resource.Error(InvalidSketchIdException())
			}

			val metaData = sketchModel.toMetaDataEntity(deviceId, timeZone)
				.copy(isDeleted = true, modifiedAt = Clock.System.now())

			val updatedContent = sketchContent
				.copy(modifiedAt = Clock.System.now(), modifiedByDeviceId = deviceId)

			val log = SketchAuditLogEntity(
				id = Uuid.random(),
				sketchId = result.id,
				changeType = SketchChangeType.DELETE,
				prevVersion = result.version,
				newVersion = result.version + 1,
				modifiedAt = metaData.modifiedAt,
				deviceId = deviceId
			)
			sketchesDao.insertSketchMetaDataAndContent(metaData, updatedContent, log)
			Resource.Success(true)
		}
	}
}