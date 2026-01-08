package com.sam.bluepad.domain.provider

import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface LocalDeviceInfoProvider {

	val readDeviceId: Flow<Uuid>
	val readDeviceInfo: Flow<LocalDeviceInfoModel>

	suspend fun updateDeviceId(): Uuid
	suspend fun updateDeviceName(newName: String)

	suspend fun initiateDeviceInfo()
}