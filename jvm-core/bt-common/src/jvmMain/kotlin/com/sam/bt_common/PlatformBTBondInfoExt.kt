package com.sam.bt_common

import com.sam.bt_common.models.BTBondResult
import com.sam.bt_common.models.BTBondState
import com.sam.bt_common.platform.PlatformBondInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun PlatformBondInfoProvider.readBondStateAsync(deviceAddress: String) = withContext(Dispatchers.IO) {
    val status = checkDeviceBondState(address = deviceAddress)
    BTBondState.fromStatus(status)
}

suspend fun PlatformBondInfoProvider.createBondAsync(deviceAddress: String) = withContext(Dispatchers.IO) {
    val status = createBond(deviceAddress, 60 * 1000)
    BTBondResult.fromInt(status)
}
