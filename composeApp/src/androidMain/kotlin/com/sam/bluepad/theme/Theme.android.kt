package com.sam.bluepad.theme

import android.os.Build
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun BluePadTheme(
    isDarkTheme: Boolean,
    dynamicColor: Boolean,
    useSystemFonts: Boolean,
    customTypography: Typography?,
    content: @Composable (() -> Unit)
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        isDarkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val typography = when {
        useSystemFonts || customTypography == null -> SystemTypography
        else -> customTypography
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
