package com.sam.bluepad.domain.bluetooth

import com.sam.bluepad.domain.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.domain.bluetooth.models.BTDeviceBondInfo
import kotlinx.coroutines.flow.Flow

interface BTDeviceBondManager {

    val isFeatureAvailable: Boolean

    val canShowConfirmPinDialog: Boolean

    suspend fun checkBondState(address: String): Result<BTDeviceBondState>

    fun requestBond(address: String): Flow<BTDeviceBondInfo>
}
