package com.sam.bt_common

import assertk.assertThat
import assertk.assertions.isTrue
import com.sam.bt_common.platform.PlatformBTInfoProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PlatformBTInfoProviderTest {

    @Test
    fun `check if we can read bluetooth is le connection is allowed`() = runTest {
        val provider = PlatformBTInfoProvider()
        provider.use { provider ->
            val isAllowed = provider.isLEConnectionAllowed()
            assertThat(isAllowed).isTrue()
        }
    }

    @Test
    fun `check if bluetooth peripheral connection supported`() = runTest {
        val provider = PlatformBTInfoProvider()
        // didn't throw error means its good
        provider.use { provider ->
            assertThat(provider.isPeripheralRoleSupported()).isTrue()
        }
    }

    @Test
    fun `check if bluetooth is active can be called`() = runTest {
        val provider = PlatformBTInfoProvider()
        // didn't throw error means its good
        provider.use { provider ->
            provider.isBluetoothActive()
        }
    }
}
