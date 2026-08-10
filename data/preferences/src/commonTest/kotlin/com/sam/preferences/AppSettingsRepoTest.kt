package com.sam.preferences

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isNotEqualTo
import com.sam.bluepad.common.utils.IFilesProvider
import com.sam.bluepad.domain.repository.IUserAppSettingsRepository
import com.sam.preferences.di.PreferencesTestModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsRepoTest : KoinTest {


    private val settingsRepo by inject<IUserAppSettingsRepository>()
    private val filesProvider by inject<IFilesProvider>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        allowOverride(true)
        module<PreferencesTestModule>()
    }

    @AfterTest
    fun cleanup() {
        // cleans up the files path
        filesProvider.deletePath()
    }

    @Test
    fun test_read_and_update_user_app_settings() = runTest {
        settingsRepo.settingsFlow.test(timeout = 5.seconds) {
            val skip = awaitItem()

            settingsRepo.toggleUseSystemFont()
            advanceUntilIdle()

            val updated = awaitItem()
            assertThat(updated.fontOption).isNotEqualTo(skip.fontOption)

            settingsRepo.toggleUseDynamicColor()
            advanceUntilIdle()

            val updated2 = awaitItem()
            assertThat(updated2.fontOption).isNotEqualTo(skip.fontOption)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
