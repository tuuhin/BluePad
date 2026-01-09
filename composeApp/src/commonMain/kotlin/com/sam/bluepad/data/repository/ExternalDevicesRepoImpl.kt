package com.sam.bluepad.data.repository

import com.sam.bluepad.data.database.dao.DevicesInfoDao
import com.sam.bluepad.data.database.entities.DeviceInfoEntity
import com.sam.bluepad.data.mappers.toDevice
import com.sam.bluepad.data.mappers.toEntity
import com.sam.bluepad.domain.exceptions.InvalidExternalDeviceIdException
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.repository.ExternalDevicesRepository
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.domain.utils.handleDBOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class ExternalDevicesRepoImpl(
	private val devicesDao: DevicesInfoDao
) : ExternalDevicesRepository {

	override fun saveOrUpdateDevice(device: ExternalDeviceModel): Flow<Resource<ExternalDeviceModel, Exception>> {
		return handleDBOperation {
			devicesDao.addDevice(device.toEntity())
			val result = devicesDao.readDeviceById(device.id) ?: run {
				return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException())
			}
			Resource.Success(result.toDevice())
		}
	}

	override fun revokeOrUnRevokeDevice(device: ExternalDeviceModel): Flow<Resource<ExternalDeviceModel, Exception>> {
		return handleDBOperation {
			val result = devicesDao.readDeviceById(device.id) ?: run {
				return@handleDBOperation Resource.Error(InvalidExternalDeviceIdException())
			}
			val copied = result.copy(isRevoked = !result.isRevoked)
			devicesDao.addDevice(copied)
			Resource.Success(copied.toDevice())
		}
	}

	override fun getExternalDevices(): Flow<Resource<List<ExternalDeviceModel>, Exception>> {
		return devicesDao.readDevices(false)
			.map<List<DeviceInfoEntity>, Resource<List<ExternalDeviceModel>, Exception>> { entities ->
				val devices = entities.map { it.toDevice() }
				Resource.Success(devices)
			}
			.onStart { emit(Resource.Loading) }
			.catch { err ->
				if (err is Exception)
					emit(Resource.Error(err))
			}
	}

	override fun getRevokedDevices(): Flow<Resource<List<ExternalDeviceModel>, Exception>> {
		return devicesDao.readDevices(true)
			.map<List<DeviceInfoEntity>, Resource<List<ExternalDeviceModel>, Exception>> { entities ->
				val devices = entities.map { it.toDevice() }
				Resource.Success(devices)
			}
			.onStart { emit(Resource.Loading) }
			.catch { err ->
				if (err is Exception)
					emit(Resource.Error(err))
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