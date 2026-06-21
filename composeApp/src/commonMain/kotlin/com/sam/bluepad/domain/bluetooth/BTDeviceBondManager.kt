package com.sam.bluepad.domain.bluetooth

import com.sam.bluepad.domain.ble.enums.BTDeviceBondState
import kotlinx.coroutines.flow.Flow

interface BTDeviceBondManager {

    val isFeatureAvailable: Boolean

    suspend fun checkBondState(address: String): Result<BTDeviceBondState>

    fun requestBond(address: String): Flow<BTDeviceBondState>
}
