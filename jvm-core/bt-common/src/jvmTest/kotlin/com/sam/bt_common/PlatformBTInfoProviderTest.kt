package com.sam.bt_common

import com.sam.bt_common.platform.PlatformBTInfoProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformBTInfoProviderTest {

    @Test
    fun `check if we can read bluetooth is le connection is allowed`() = runTest {
        val provider = PlatformBTInfoProvider()
        provider.use { provider ->
            val isAllowed = provider.isLEConnectionAllowed()
            assertEquals(true, isAllowed)
        }
    }

    @Test
    fun `check if bluetooth peripheral connection supported`() = runTest {
        val provider = PlatformBTInfoProvider()
        // didn't throw error means its good
        provider.use { provider ->
            assertEquals(true, provider.isPeripheralRoleSupported())
        }
    }

    @Test
    fun `check if bluetooth is active can be called`() = runTest {
        val provider = PlatformBTInfoProvider()
        // didn't throw error means its good
        provider.use { provider -> provider.isBluetoothActive() }
    }
}
