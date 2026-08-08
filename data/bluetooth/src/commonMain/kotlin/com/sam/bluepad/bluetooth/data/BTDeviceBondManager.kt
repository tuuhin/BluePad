package com.sam.bluepad.bluetooth.data

import com.sam.bluepad.domain.bluetooth.IBTDeviceBondManager
import com.sam.bluepad.model.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.model.bluetooth.models.BTDeviceBondInfo
import kotlinx.coroutines.flow.Flow

internal expect class BTDeviceBondManager : IBTDeviceBondManager {


    override val canShowConfirmPinDialog: Boolean
    override val isFeatureAvailable: Boolean

    override fun requestBond(address: String): Flow<BTDeviceBondInfo>

    override suspend fun acceptBondConfirmationPin(pin: String): Result<Unit>

    override suspend fun checkBondState(address: String): Result<BTDeviceBondState>
}
