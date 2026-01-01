package com.sam.bluepad

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun App() {
	Surface(
		color = MaterialTheme.colorScheme.background,
		modifier = Modifier.fillMaxSize()
	) {
		Text("Hi")
	}
}
