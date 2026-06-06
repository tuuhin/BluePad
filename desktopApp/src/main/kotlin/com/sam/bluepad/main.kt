package com.sam.bluepad

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.koin.KermitKoinLogger
import com.sam.bluepad.desktop.resources.Res
import com.sam.bluepad.desktop.resources.app_name
import com.sam.bluepad.desktop.resources.ic_notepad
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.viewModelModule
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.theme.BluePadTheme
import io.github.kdroidfilter.nucleus.window.material.MaterialDecoratedWindow
import io.github.kdroidfilter.nucleus.window.material.MaterialTitleBar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

fun main() = application {

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


@Composable
private fun ApplicationScope.NucleusWindowWrapper(
    content: @Composable () -> Unit
) {
    val windowState = rememberWindowState(position = WindowPosition(Alignment.Center))

    MaterialDecoratedWindow(
        onCloseRequest = ::exitApplication,
        icon = painterResource(Res.drawable.ic_notepad),
        title = stringResource(Res.string.app_name),
        state = windowState,
    ) {
        MaterialTitleBar {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        //Your app content
        content()
    }
}
