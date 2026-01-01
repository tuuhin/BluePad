package com.sam.bluepad

import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {

	System.setProperty("skiko.renderApi", "OPENGL")

	val windowState = rememberWindowState(position = WindowPosition(Alignment.Center))

    Window(
		state = windowState,
        onCloseRequest = ::exitApplication,
        title = "BluePad",
    ) {
        App()
    }
}