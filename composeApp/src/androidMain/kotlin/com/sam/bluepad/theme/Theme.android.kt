package com.sam.bluepad.theme

import android.os.Build
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
actual fun BluePadTheme(
    isDarkTheme: Boolean,
    dynamicColor: Boolean,
    useSystemFonts: Boolean,
    customTypography: Typography?,
    content: @Composable (() -> Unit)
) {
    val context = LocalContext.current

    val dynamicThemeState = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val colorScheme = if (isDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
            rememberDynamicMaterialThemeState(
                isDark = isDarkTheme,
                primary = colorScheme.primary,
                secondary = colorScheme.secondary,
                tertiary = colorScheme.tertiary,
                error = colorScheme.error,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            )
        }

        else -> rememberDynamicMaterialThemeState(
            isDark = isDarkTheme,
            style = PaletteStyle.TonalSpot,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            seedColor = SeedColor,
        )
    }


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
