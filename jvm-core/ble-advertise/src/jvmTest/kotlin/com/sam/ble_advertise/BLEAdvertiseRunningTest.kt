package com.sam.ble_advertise

import com.sam.ble_advertise.platform.BLEAdvertisementStatus
import com.sam.ble_advertise.platform.GATTAdvertiseConfig
import com.sam.ble_advertise.platform.PlatformBLEAdvertiser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BLEAdvertiseRunningTest {

    @Test
    fun `just start the advertisement for few seconds to check if all good or not`() = runTest {
        val advertise = PlatformBLEAdvertiser()
        try {
            advertise.start(config = GATTAdvertiseConfig(discoverable = true, connectable = true))
        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }

    @Test
    fun `read a ble advertisement status`() = runTest {
        val advertise = PlatformBLEAdvertiser()
        try {
            assertEquals(
                BLEAdvertisementStatus.Aborted,
                advertise.getStatus(),
                message = "ADVERTISEMENT STATUS WILL HAVE AN ABORTED BASE STATE",
            )
        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }
}
