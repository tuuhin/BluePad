package com.sam.preferences

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import com.sam.bluepad.common.models.PlatformOS
import com.sam.bluepad.common.utils.IFilesProvider
import com.sam.bluepad.domain.repository.ILocalDeviceRepository
import com.sam.preferences.di.PreferencesTestModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class LocalDeviceRepoTest : KoinTest {


    private val localDeviceRepo by inject<ILocalDeviceRepository>()
    private val filesProvider by inject<IFilesProvider>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        allowOverride(true)
        module<PreferencesTestModule>()
    }

    @BeforeTest
    fun setup() = runBlocking {
        // ensures we some device data to continue with
        localDeviceRepo.initiateDeviceInfo()
    }

    @AfterTest
    fun cleanup() {
        // cleans up the files path
        filesProvider.deletePath()
    }

    @Test
    fun test_reading_and_updating_the_local_device_data() = runTest {

        localDeviceRepo.readDeviceInfo.test(timeout = 5.seconds) {
            val firstItem = awaitItem()
            assertThat(firstItem.platformOS).isNotEqualTo(PlatformOS.UNKNOWN)
            assertThat(firstItem.aliasName).isNotEmpty()

            val newName = "Some New Name"
            localDeviceRepo.updateDeviceName(newName)
            advanceUntilIdle()

            val secondItem = awaitItem()
            assertThat(secondItem.aliasName).isEqualTo(newName)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
