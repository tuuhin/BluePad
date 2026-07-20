package com.sam.bluepad

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.viewModelModule
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTestRule

class IsDesktopAppRunnableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(createPlatformModule(), commonAppModule, viewModelModule)
        logger(KermitKoinLogger(Logger.withTag("TEST")))
    }

    @Test
    fun testAppShows() {
        composeTestRule.setContent {
            App()
        }
        composeTestRule.onRoot().assertExists()
    }
}
