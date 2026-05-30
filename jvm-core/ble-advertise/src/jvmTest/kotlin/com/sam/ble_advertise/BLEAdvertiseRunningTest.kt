package com.sam.ble_advertise

import com.sam.ble_advertise.platform.GATTAdvertiseConfig
import com.sam.ble_advertise.platform.PlatformBLEAdvertiser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BLEAdvertiseRunningTest {

    @Test
    fun `just start the advertisement for few seconds to check if all good or not`() = runTest {
        val advertise = PlatformBLEAdvertiser()
        try {
            advertise.start(config = GATTAdvertiseConfig(discoverable = true, connectable = true,),)
        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }
}
