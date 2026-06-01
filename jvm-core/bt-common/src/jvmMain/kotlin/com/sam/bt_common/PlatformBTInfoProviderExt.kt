package com.sam.bt_common

import com.sam.bt_common.platform.PlatformBTInfoProvider

val PlatformBTInfoProvider.Companion.isBTActive: Boolean
    get() = PlatformBTInfoProvider().use { provider -> provider.isBluetoothActive() }

val PlatformBTInfoProvider.Companion.isLEConnectionAvailable: Boolean
    get() = PlatformBTInfoProvider().use { provider -> provider.isLEConnectionAllowed() }

val PlatformBTInfoProvider.Companion.isPeripheralRoleSupported: Boolean
    get() = PlatformBTInfoProvider().use { provider -> provider.isPeripheralRoleSupported() }
