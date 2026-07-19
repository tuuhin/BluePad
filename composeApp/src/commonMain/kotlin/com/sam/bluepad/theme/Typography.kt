package com.sam.bluepad.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

internal expect val SystemFontFamily: FontFamily

internal val AppTypography: Typography
    get() = Typography(fontFamily = SystemFontFamily)
