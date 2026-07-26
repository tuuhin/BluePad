package com.sam.ble_advertise

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.sam.ble_advertise.extension.addService
import com.sam.ble_advertise.extension.getStatus
import com.sam.ble_advertise.extension.setListener
import com.sam.ble_advertise.models.BLEAdvertisementStatus
import com.sam.ble_advertise.models.Characteristic
import com.sam.ble_advertise.models.Service
import com.sam.ble_advertise.platform.GATTAdvertiseConfig
import com.sam.ble_advertise.platform.PlatformBLEAdvertiser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class BLEAdvertiseRunningTest {

    @Test
    fun `start the advertisement for few seconds without any services`() = runBlocking {
        val advertise = PlatformBLEAdvertiser()
        try {
            advertise.start(config = GATTAdvertiseConfig(discoverable = true, connectable = true))
            // a bit of a wait these are hardware calls a little delay is better
            delay(100.milliseconds)
            advertise.stop()
        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }

    @Test
    fun `read the service status without any other calls`() = runTest {
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
    fun `test just stoping and  destroying the service without handing anything`() = runTest {
        val advertise = PlatformBLEAdvertiser()
        advertise.use { advertise ->
            advertise.stop()
            advertise.onDestroy()
        }
    }

    @Test
    fun `start the ble advertisement with some characteristic and service without a listener`() = runTest {
        val advertise = PlatformBLEAdvertiser()
        try {
            assertThat(advertise.getStatus(), name = "status should be unknown for the first try")
                .isEqualTo(BLEAdvertisementStatus.Unknown)

            // random service
            val service = Service.builder(Uuid.random())
                .addCharacteristic(Characteristic.builder(Uuid.random()).build())
                .build()

            advertise.addService(service)

            advertise.start(
                config = GATTAdvertiseConfig(
                    discoverable = true,
                    connectable = true,
                    serviceData = "some data",
                ),
            )
            delay(100.milliseconds)

            assertThat(advertise.getStatus(), name = "Should fail as there is no listener to handle status response")
                .isEqualTo(BLEAdvertisementStatus.Unknown)

            // stop it
            advertise.stop()

        } finally {
            advertise.onDestroy()
            advertise.close()
        }
    }


    @Test
    fun `start the ble advertisement with some characteristic and service with a listener`() = runBlocking {
        val advertise = PlatformBLEAdvertiser()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            assertThat(advertise.getStatus(), name = "status should be unknown for the first try")
                .isEqualTo(BLEAdvertisementStatus.Unknown)

            advertise.setListener(BLEAdvertiseTestListener, scope)

            // random service
            val service = Service.builder(Uuid.random())
                .addCharacteristic(Characteristic.builder(Uuid.random()).build())
                .build()

            advertise.addService(service)

            advertise.start(
                config = GATTAdvertiseConfig(
                    discoverable = true,
                    connectable = true,
                    serviceData = "some data",
                ),
            )
            delay(100.milliseconds)

            assertThat(advertise.getStatus(), name = "Should have started by now")
                .isEqualTo(BLEAdvertisementStatus.StartedWithoutAdvertisementData)

            // stop it
            advertise.stop()

        } finally {
            scope.cancel()
            advertise.onDestroy()
            advertise.close()
        }
    }
}
