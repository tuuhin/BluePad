package com.sam.bluepad.data.repository

import com.sam.bluepad.data.database.dao.DevicesInfoDao
import com.sam.bluepad.data.database.entities.DeviceInfoEntity
import com.sam.bluepad.data.mappers.toDevice
import com.sam.bluepad.data.mappers.toEntity
import com.sam.bluepad.domain.exceptions.InvalidExternalDeviceIdException
import com.sam.bluepad.domain.exceptions.NoRevokedDeviceFoundException
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.repository.ResourceExternalDevice
import com.sam.bluepad.domain.repository.ResourceExternalDeviceList
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.domain.utils.handleDBOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.uuid.Uuid

class ExternalDevicesRepoImpl(
    private val devicesDao: DevicesInfoDao
) : ExternalDevicesRepository {

    override fun saveOrUpdateDevice(device: ExternalDeviceModel, keepRevoked: Boolean)
            : Flow<ResourceExternalDevice> {
        return handleDBOperation {

            // if the entity is revoked don't change it
            val entity = if (keepRevoked) device.toEntity()
            else device.toEntity().copy(isRevoked = false)

            devicesDao.insertOrUpdateDevice(entity)

            val result = devicesDao.readDeviceById(device.id)
                ?: return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException())

            Resource.Success(result.toDevice())
        }
    }

    override fun saveOrUpdateDevices(devices: List<ExternalDeviceModel>, keepRevoked: Boolean)
            : Flow<ResourceExternalDeviceList> {
        return handleDBOperation {
            // if the entity is revoked don't change it
            val entities = devices.map { device ->
                if (keepRevoked) device.toEntity()
                else device.toEntity().copy(isRevoked = false)
            }

            devicesDao.insertOrUpdateDevices(entities)
            val deviceIds = entities.map { infoEntity -> infoEntity.id }
            val updatedDevices = devicesDao.readDevicesByIds(deviceIds)
                .map { infoEntity -> infoEntity.toDevice() }

            Resource.Success(updatedDevices)
        }
    }

    override fun revokeOrUnRevokeDevice(device: ExternalDeviceModel): Flow<Resource<ExternalDeviceModel, Exception>> {
        return handleDBOperation {
            val result = devicesDao.readDeviceById(device.id)
                ?: return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException())

            devicesDao.setRevokeStatusOnDeviceByID(
                newRevokeStatus = !result.isRevoked,
                deviceId = result.id
            )

            val updatedDevice = devicesDao.readDeviceById(result.id)
                ?: return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException())

            Resource.Success(updatedDevice.toDevice())
        }
    }

    override fun getAllDevices(): Flow<ResourceExternalDeviceList> {
        return devicesDao.readAllDevices(false)
            .map<List<DeviceInfoEntity>, ResourceExternalDeviceList> { entities ->
                val devices = entities.map { it.toDevice() }
                Resource.Success(devices)
            }
            .onStart { emit(Resource.Loading) }
            .catch { err ->
                if (err is Exception)
                    emit(Resource.Error(err))
            }
    }

    override fun getAllRevokedDevices(): Flow<Resource<List<ExternalDeviceModel>, Exception>> {
        return devicesDao.readAllDevices(true)
            .map<List<DeviceInfoEntity>, ResourceExternalDeviceList> { entities ->
                val devices = entities.map { it.toDevice() }
                Resource.Success(devices)
            }
            .onStart { emit(Resource.Loading) }
            .catch { err ->
                if (err is Exception)
                    emit(Resource.Error(err))
            }
    }


    override suspend fun getDeviceByUuid(uuid: Uuid): Result<ExternalDeviceModel> {
        return try {
            val result = devicesDao.readDeviceById(uuid)
                ?: return Result.failure(InvalidExternalDeviceIdException())
            return Result.success(result.toDevice())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun unRevokeAllDevices(): Flow<Resource<Unit, Exception>> {
        return handleDBOperation {
            val rowsUpdated = devicesDao.reEnrollAllDevice()
            if (rowsUpdated == 0)
                return@handleDBOperation Resource.Error(NoRevokedDeviceFoundException())
            Resource.Success(Unit)
        }
    }

    override fun deleteDevice(device: ExternalDeviceModel): Flow<Resource<Unit, Exception>> {
        return handleDBOperation {
            devicesDao.deleteDevice(device.toEntity())
            Resource.Success(Unit)
        }
    }

    override fun deleteDevices(devices: List<ExternalDeviceModel>): Flow<Resource<Unit, Exception>> {
        return handleDBOperation {
            val entities = devices.map { it.toEntity() }
            devicesDao.deleteDevices(entities)
            Resource.Success(Unit)
        }
    }

}