package com.sam.bluepad.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

@Composable
actual fun BluePadTheme(
	isDarkTheme: Boolean,
	dynamicColor: Boolean,
	content: @Composable (() -> Unit)
) {
	val colorScheme = when {
		isDarkTheme -> darkColorScheme
		else -> lightColorScheme
	}

	MaterialExpressiveTheme(
		colorScheme = colorScheme,
		typography = AppTypography,
		motionScheme = MotionScheme.expressive(),
		content = content
	)
}