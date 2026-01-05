package com.sam.bluepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sam.bluepad.theme.BluePadTheme

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		// edge to edge
		installSplashScreen()
		super.onCreate(savedInstanceState)

		enableEdgeToEdge()

		setContent {
			BluePadTheme {
				App()
			}
		}
	}
}
