package com.sam.bluepad.repository.impl

import com.sam.bluepad.database.data.dao.DevicesInfoDao
import com.sam.bluepad.database.data.entities.DeviceInfoEntity
import com.sam.bluepad.domain.repository.IExternalDeviceRepository
import com.sam.bluepad.domain.repository.ResourceExternalDevice
import com.sam.bluepad.domain.repository.ResourceExternalDeviceList
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.model.devices.ExternalDeviceModel
import com.sam.bluepad.repository.exceptions.InvalidExternalDeviceIdException
import com.sam.bluepad.repository.exceptions.NoRevokedDeviceFoundException
import com.sam.bluepad.repository.mapper.toDevice
import com.sam.bluepad.repository.mapper.toEntity
import com.sam.bluepad.repository.util.handleDBOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory(binds = [IExternalDeviceRepository::class])
internal class ExternalDevicesRepoImpl(
    private val devicesDao: DevicesInfoDao
) : IExternalDeviceRepository {


    override fun saveOrUpdateDevice(device: ExternalDeviceModel, keepRevoked: Boolean)
        : Flow<ResourceExternalDevice> {
        return handleDBOperation {

            // if the entity is revoked don't change it
            val entity = if (keepRevoked) device.toEntity()
            else device.toEntity().copy(isRevoked = false)

            devicesDao.insertOrUpdateDevice(entity)

            val result = devicesDao.readDeviceById(device.id)
                ?: return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException(device.id))

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
                ?: return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException(device.id))

            devicesDao.setRevokeStatusOnDeviceByID(
                newRevokeStatus = !result.isRevoked,
                deviceId = result.id,
            )

            val updatedDevice = devicesDao.readDeviceById(result.id)
                ?: return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException(result.id))

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
                ?: return Result.failure(InvalidExternalDeviceIdException(uuid))
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
