package com.sam.bluepad.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import dev.nucleusframework.systemcolor.systemAccentColor

@Composable
actual fun BluePadTheme(
    isDarkTheme: Boolean,
    dynamicColor: Boolean,
    useSystemFonts: Boolean,
    customTypography: Typography?,
    content: @Composable (() -> Unit)
) {
    val accentColor = systemAccentColor()
    val seedColor = if (accentColor != null && dynamicColor) accentColor else SeedColor

    val dynamicThemeState = rememberDynamicMaterialThemeState(
        isDark = isDarkTheme,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        seedColor = seedColor,
    )

    val typography = when {
        useSystemFonts || customTypography == null -> SystemTypography
        else -> customTypography
    }

    DynamicMaterialExpressiveTheme(
        state = dynamicThemeState,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        animate = true,
        animationSpec = tween(durationMillis = 200, easing = EaseInOut),
        content = content,
    )
}
