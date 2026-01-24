package com.sam.bluepad.domain.repository

import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

typealias ResourceExternalDevice = Resource<ExternalDeviceModel, Exception>
typealias ResourceExternalDeviceList = Resource<List<ExternalDeviceModel>, Exception>

interface ExternalDevicesRepository {

	fun saveOrUpdateDevice(device: ExternalDeviceModel, keepRevoked: Boolean = true)
			: Flow<ResourceExternalDevice>

	fun saveOrUpdateDevices(devices: List<ExternalDeviceModel>, keepRevoked: Boolean = true)
			: Flow<ResourceExternalDeviceList>

	fun revokeOrUnRevokeDevice(device: ExternalDeviceModel): Flow<ResourceExternalDevice>
	fun unRevokeAllDevices(): Flow<Resource<Unit, Exception>>

	fun getAllDevices(): Flow<ResourceExternalDeviceList>
	fun getAllRevokedDevices(): Flow<ResourceExternalDeviceList>

	fun deleteDevice(device: ExternalDeviceModel): Flow<Resource<Unit, Exception>>

	fun deleteDevices(devices: List<ExternalDeviceModel>): Flow<Resource<Unit, Exception>>
}