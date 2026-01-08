package com.sam.bluepad

import androidx.compose.runtime.Composer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.viewModelModule
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.app_name
import com.sam.bluepad.resources.scratchpad
import com.sam.bluepad.theme.BluePadTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

fun main() = application {
	Logger.setMinSeverity(if (BuildKonfig.IS_DEBUG) Severity.Debug else Severity.Info)
	// start koin
	startKoin {
		printLogger(Level.INFO)
		// Modules
		modules(createPlatformModule(), commonAppModule, viewModelModule)
	}
	// compose stack trace
	if (BuildKonfig.IS_DEBUG)
		Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)

	// WARN: NEED THIS TO RUN ON MY MACHINE IF NOT NEEDED CANNOT BE COMMENTED OUT
	System.setProperty("skiko.renderApi", "OPENGL")
	val deviceInfoProvider = koinInject<LocalDeviceInfoProvider>()

	// APPLICATION CODE
	val windowState = rememberWindowState(
		position = WindowPosition(Alignment.Center),
	)

	//initiate device data
	LaunchedEffect(Unit) {
		deviceInfoProvider.initiateDeviceInfo()
	}

	// finally we create the window
	Window(
		state = windowState,
		onCloseRequest = ::exitApplication,
		icon = painterResource(Res.drawable.scratchpad),
		title = stringResource(Res.string.app_name)
	) {
		BluePadTheme {
			App()
		}
	}
}