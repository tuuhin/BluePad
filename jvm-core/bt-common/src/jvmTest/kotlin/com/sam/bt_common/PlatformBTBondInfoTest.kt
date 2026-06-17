package com.sam.bt_common

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.sam.bt_common.models.BTBondState
import com.sam.bt_common.platform.PlatformBondInfoProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PlatformBTBondInfoTest {

    @Test
    fun `check can random mac id user to read bond state should fail as error invalid device`() = runTest {
        val randomMacAddress = "63:42:05:24:D2:52"
        PlatformBondInfoProvider().use { provider ->
            if (!provider.canReadBondInfo) return@use
            val status = provider.readBondStateAsync(randomMacAddress)
            assertThat(status).isEqualTo(BTBondState.ERROR_INVALID_DEVICE)
        }
    }

    @Test
    fun `check can a random string be used to check bond state should result in unknown error`() = runTest {
        val randomMacAddress = "abcdefghijkl"
        PlatformBondInfoProvider().use { provider ->
            if (!provider.canReadBondInfo) return@use
            val status = provider.readBondStateAsync(randomMacAddress)
            assertThat(status).isEqualTo(BTBondState.ERROR_UNKNOWN)
        }
    }

}
