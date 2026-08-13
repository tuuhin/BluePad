package com.sam.bluepad.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp

internal val SystemTypography: Typography
    @Composable
    get() {
        val font = systemFontFamily() ?: return Typography()
        return Typography(font)
    }

val AppTypographyCustom: Typography
    @Composable
    get() {
        // --- Font Configurations ---
        val displayFont = googleSansFlexFont(
            FontVariation.Settings(
                FontVariation.weight(800),
                FontVariation.slant(0f),
                FontVariation.width(110f),
            ),
        )
        val headlineFont = googleSansFlexFont(
            FontVariation.Settings(
                FontVariation.weight(700),
                FontVariation.slant(0f),
                FontVariation.width(100f),
            ),
        )
        val bodyFont = robotoFlexFont(
            FontVariation.Settings(
                FontVariation.weight(400),
                FontVariation.slant(0f),
                FontVariation.width(100f),
            ),
        )
        val labelFont = robotoFlexFont(
            FontVariation.Settings(
                FontVariation.weight(400),
                FontVariation.slant(0f),
                FontVariation.width(105f),
            ),
        )
        val heavyEmphasisFont = googleSansFlexFont(
            FontVariation.Settings(
                FontVariation.weight(900), // Clamped to 900 for standard variable font safety
                FontVariation.slant(0f),
                FontVariation.width(120f),
            ),
        )

        return Typography(
            // --- Standard Display Styles ---
            displayLarge = TextStyle(
                fontFamily = displayFont,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = displayFont,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
            displaySmall = TextStyle(
                fontFamily = displayFont,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),

            // --- Standard Headline Styles ---
            headlineLarge = TextStyle(
                fontFamily = headlineFont,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = headlineFont,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = headlineFont,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),

            // --- Standard Title Styles ---
            titleLarge = TextStyle(
                fontFamily = headlineFont,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = googleSansFlexFont(
                    settings = FontVariation.Settings(FontVariation.weight(500)), // Fixed ultra-thin weight (150 -> 500)
                ),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = googleSansFlexFont(
                    settings = FontVariation.Settings(FontVariation.weight(500)),
                ),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),

            // --- Standard Body Styles ---
            bodyLarge = TextStyle(
                fontFamily = bodyFont,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = bodyFont,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = robotoFlexFont(
                    settings = FontVariation.Settings(FontVariation.weight(300)),
                ),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),

            // --- Standard Label Styles ---
            labelLarge = TextStyle(
                fontFamily = labelFont,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = labelFont,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = labelFont,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),

            // --- Material 3 Expressive Emphasized Styles ---
            displayLargeEmphasized = TextStyle(
                fontFamily = heavyEmphasisFont,
                fontSize = 64.sp,
                lineHeight = 72.sp,
                letterSpacing = (-0.5).sp,
            ),
            displayMediumEmphasized = TextStyle(
                fontFamily = heavyEmphasisFont,
                fontSize = 50.sp,
                lineHeight = 56.sp,
                letterSpacing = (-0.25).sp,
            ),
            headlineLargeEmphasized = TextStyle(
                fontFamily = heavyEmphasisFont,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                letterSpacing = 0.sp,
            ),
            titleLargeEmphasized = TextStyle(
                fontFamily = heavyEmphasisFont,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        )
    }
