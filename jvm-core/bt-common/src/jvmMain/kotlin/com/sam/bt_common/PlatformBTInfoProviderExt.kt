package com.sam.bt_common

import com.sam.bt_common.models.BTJVMEnableResult
import com.sam.bt_common.platform.PlatformBTInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun PlatformBTInfoProvider.Companion.isBTActive(): Boolean = withContext(Dispatchers.IO) {
    PlatformBTInfoProvider().use { provider -> provider.isBluetoothActive() }
}

suspend fun PlatformBTInfoProvider.Companion.isLEConnectionAvailable(): Boolean = withContext(Dispatchers.IO) {
    PlatformBTInfoProvider().use { provider -> provider.isLEConnectionAllowed() }
}

suspend fun PlatformBTInfoProvider.Companion.isPeripheralRoleSupported(): Boolean = withContext(Dispatchers.IO) {
    PlatformBTInfoProvider().use { provider -> provider.isPeripheralRoleSupported() }
}

suspend fun PlatformBTInfoProvider.Companion.requestBTEnableAsync() = withContext(Dispatchers.IO) {
    val resp = PlatformBTInfoProvider().use { it.requestBTEnable() }
    return@withContext BTJVMEnableResult.fromInt(resp)
}
