package com.sam.bluepad

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sam.bluepad.di.commonAppModule
import com.sam.bluepad.di.createPlatformModule
import com.sam.bluepad.di.viewModelModule
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.app_name
import com.sam.bluepad.theme.BluePadTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

fun main() = application {
	// WARN: NEED THIS TO RUN ON MY MACHINE IF NOT NEEDED CANNOT BE COMMENTED OUT
	System.setProperty("skiko.renderApi", "OPENGL")

	val windowState = rememberWindowState(position = WindowPosition(Alignment.Center))

	// start koin
	startKoin {
		printLogger(Level.INFO)
		// Modules
		modules(createPlatformModule(), commonAppModule, viewModelModule)
	}

	Window(
		state = windowState,
		onCloseRequest = ::exitApplication,
		title = stringResource(Res.string.app_name)
	) {
		BluePadTheme {
			App()
		}
	}
}