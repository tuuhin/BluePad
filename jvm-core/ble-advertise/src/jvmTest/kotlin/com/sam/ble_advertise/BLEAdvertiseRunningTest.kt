package com.sam.ble_advertise

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isIn
import com.sam.ble_advertise.platform.BLEAdvertisementStatus
import com.sam.ble_advertise.platform.GATTAdvertiseConfig
import com.sam.ble_advertise.platform.PlatformBLEAdvertiser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BLEAdvertiseRunningTest {

    @Test
    fun `just start the advertisement for few seconds to check if all good or not`() = runBlocking {
        val advertise = PlatformBLEAdvertiser()
        try {
            advertise.start(config = GATTAdvertiseConfig(discoverable = true, connectable = true))
            // a bit of a wait these are hardware calls a little delay is better
            delay(100L)
            advertise.stop()
        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }

    @Test
    fun `read a ble advertisement status`() = runTest {
        val advertise = PlatformBLEAdvertiser()
        try {
            assertThat(advertise.getStatus(), name = "status should be unknown for the first try")
                .isEqualTo(BLEAdvertisementStatus.Unknown)
        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }

    @Test
    fun `try to start the ble advertisement with some characteristic and service`() = runBlocking {
        val advertise = PlatformBLEAdvertiser()
        try {
            assertThat(advertise.getStatus(), name = "status should be unknown for the first try")
                .isEqualTo(BLEAdvertisementStatus.Unknown)

            advertise.start(
                config = GATTAdvertiseConfig(
                    discoverable = true,
                    connectable = true,
                    serviceData = "some data",
                ),
            )
            delay(100L)

            assertThat(advertise.getStatus(), name = "Should have started ")
                .isIn(
                    BLEAdvertisementStatus.StartedWithoutAdvertisementData,
                    BLEAdvertisementStatus.Started,
                )
            advertise.stop()
        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }
}
