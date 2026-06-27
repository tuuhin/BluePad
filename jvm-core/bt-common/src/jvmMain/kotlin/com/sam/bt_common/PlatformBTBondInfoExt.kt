package com.sam.bt_common

import com.sam.bt_common.models.BTJVMBondState
import com.sam.bt_common.platform.PlatformBondInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun PlatformBondInfoProvider.readBondStateAsync(deviceAddress: String) = withContext(Dispatchers.IO) {
    val status = checkDeviceBondState(address = deviceAddress)
    BTJVMBondState.fromStatus(status)
}
