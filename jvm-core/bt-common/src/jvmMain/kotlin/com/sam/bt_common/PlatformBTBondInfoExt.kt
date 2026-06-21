package com.sam.bt_common

import com.sam.bt_common.models.BTJVMBondResult
import com.sam.bt_common.models.BTJVMBondState
import com.sam.bt_common.platform.PlatformBondInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

suspend fun PlatformBondInfoProvider.readBondStateAsync(deviceAddress: String) = withContext(Dispatchers.IO) {
    val status = checkDeviceBondState(address = deviceAddress)
    BTJVMBondState.fromStatus(status)
}

suspend fun PlatformBondInfoProvider.createBondAsync(deviceAddress: String, timeout: Duration = 1.minutes) =
    withContext(Dispatchers.IO) {
        val status = createBond(deviceAddress, timeout.inWholeMilliseconds.toInt())
        BTJVMBondResult.fromInt(status)
    }
