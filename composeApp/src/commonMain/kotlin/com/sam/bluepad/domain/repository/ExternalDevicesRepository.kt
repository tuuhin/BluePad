package com.sam.bluepad.domain.repository

import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ExternalDevicesRepository {

	fun saveOrUpdateDevice(
		device: ExternalDeviceModel,
		unRevokeOnUpdate: Boolean = true
	): Flow<Resource<ExternalDeviceModel, Exception>>

	fun toggleDeviceRevocation(device: ExternalDeviceModel): Flow<Resource<ExternalDeviceModel, Exception>>
	fun unRevokeAllDevices(): Flow<Resource<Unit, Exception>>

	fun getAllDevices(): Flow<Resource<List<ExternalDeviceModel>, Exception>>
	fun getRevokedDevices(): Flow<Resource<List<ExternalDeviceModel>, Exception>>

	fun deleteDevice(device: ExternalDeviceModel): Flow<Resource<Unit, Exception>>

	fun deleteDevices(devices: List<ExternalDeviceModel>): Flow<Resource<Unit, Exception>>
}