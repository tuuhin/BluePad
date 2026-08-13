package com.sam.bluepad.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import co.touchlab.kermit.Logger
import com.sam.bluepad.platform.common_utils.PlatformFontProviderImpl
import com.sam.bluepad.resources.GoogleSansFlex
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.RobotoFlex
import org.jetbrains.compose.resources.Font

@Composable
actual fun googleSansFlexFont(
    settings: FontVariation.Settings
): FontFamily = FontFamily(
    Font(
        resource = Res.font.GoogleSansFlex,
        variationSettings = settings,
    ),
)

@Composable
actual fun robotoFlexFont(
    settings: FontVariation.Settings
): FontFamily = FontFamily(
    Font(
        resource = Res.font.RobotoFlex,
        variationSettings = settings,
    ),
)

@OptIn(ExperimentalTextApi::class)
@Composable
actual fun systemFontFamily(): FontFamily? {
    val font = remember {
        PlatformFontProviderImpl().use { provider ->
            try {
                provider.readFontFamily()?.let(::FontFamily)
            } catch (e: Exception) {
                Logger.e(throwable = e) { "FAILED TO READ SYSTEM FONT" }
                null
            }
        }
    }
    return font
}
