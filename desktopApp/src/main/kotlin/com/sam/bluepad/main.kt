package com.sam.bluepad

import androidx.compose.runtime.Composer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.window.application
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.koin.KermitKoinLogger
import com.sam.bluepad.composables.NucleusWindowWrapper
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.viewModelModule
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.utils.TimestampMessageWriter
import com.sam.bluepad.utils.setupNativeLibraries
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

fun main() = application {

    // some internal setup to set libraries
    setupNativeLibraries()

    // logging configuration
    Logger.setMinSeverity(if (BuildKonfig.IS_DEBUG) Severity.Debug else Severity.Info)
    Logger.setLogWriters(CommonWriter(messageStringFormatter = TimestampMessageWriter))
    Logger.setTag("BLUE_PAD")

    // compose stack trace
    if (BuildKonfig.IS_DEBUG)
        Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)

    // finally we create the window
    KoinApplication(
        configuration = koinConfiguration {
            modules(createPlatformModule(), commonAppModule, viewModelModule)
            logger(KermitKoinLogger(Logger.withTag("KOIN")))
            // Modules
        },
    ) {

        val deviceInfoProvider = koinInject<LocalDeviceInfoProvider>()
        LaunchedEffect(Unit) {
            deviceInfoProvider.initiateDeviceInfo()
        }

        // APPLICATION CODE
        BluePadTheme(dynamicColor = true) {
            NucleusWindowWrapper {
                App()
            }
        }
    }
}
