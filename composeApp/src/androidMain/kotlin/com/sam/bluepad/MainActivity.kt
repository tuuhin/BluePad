package com.sam.bluepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

	
	override fun onCreate(savedInstanceState: Bundle?) {
		// edge to edge
		enableEdgeToEdge()

		super.onCreate(savedInstanceState)

		setContent {
			App()
		}
	}
}
