package com.sam.bluepad.domain.repository

import com.sam.bluepad.model.devices.LocalDeviceModel
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface ILocalDeviceRepository {


    val readDeviceInfo: Flow<LocalDeviceModel>

    suspend fun updateDeviceId(): Uuid
    suspend fun updateDeviceName(newName: String)

    suspend fun initiateDeviceInfo()
}
