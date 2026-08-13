package com.sam.bluepad.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

@Composable
expect fun BluePadTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    useSystemFonts: Boolean = true,
    customTypography: Typography? = AppTypographyCustom,
    content: @Composable () -> Unit,
)
