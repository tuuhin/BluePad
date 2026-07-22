package com.sam.bluepad

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import co.touchlab.kermit.Logger
import co.touchlab.kermit.koin.KermitKoinLogger
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.viewModelModule
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTestRule


@OptIn(ExperimentalTestApi::class)
class IsDesktopAppRunnableTest {


    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(createPlatformModule(), commonAppModule, viewModelModule)
        logger(KermitKoinLogger(Logger.withTag("TEST")))
    }

    @Test
    fun testAppShows() = runDesktopComposeUiTest {
        setContent {
            App()
        }
        onRoot().assertExists()
    }
}
