package com.sam.bluepad.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.sam.bluepad.library.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val googleSansFlex = GoogleFont("Google Sans Flex")
private val robotoFlex = GoogleFont("Roboto Flex")


@Composable
actual fun googleSansFlexFont(
    settings: FontVariation.Settings
): FontFamily = FontFamily(
    Font(
        googleSansFlex,
        fontProvider = provider,
        variationSettings = settings,
    ),
)


@Composable
actual fun robotoFlexFont(
    settings: FontVariation.Settings
): FontFamily = FontFamily(
    Font(
        robotoFlex,
        fontProvider = provider,
        variationSettings = settings,
    ),
)

@Composable
actual fun systemFontFamily(): FontFamily? = FontFamily.Default
