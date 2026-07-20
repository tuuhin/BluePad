package com.sam.bluepad.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import com.sam.bluepad.resources.GoogleSansFlex
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.RobotoFlex
import org.jetbrains.compose.resources.Font

internal expect val SystemFontFamily: FontFamily

internal val SystemTypography: Typography
    @Composable
    get() = Typography(fontFamily = SystemFontFamily)

val AppTypographyCustom: Typography
    @Composable
    get() {
        val displayFont = flexFontEmphasis(fontWeight = 800, fontWidth = 110f)
        val headlineFont = flexFontEmphasis(fontWeight = 700, fontWidth = 100f)
        val bodyFont = robotoFlex(weight = 400)
        val labelFont = robotoFlex(weight = 500, width = 105f)

        val heavyEmphasisFont = flexFontEmphasis()
        val roundedFont = flexFontRounded()

        return Typography(
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

            // Title Styles
            titleLarge = TextStyle(
                fontFamily = headlineFont,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = flexFontEmphasis(fontWeight = 550),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = flexFontEmphasis(fontWeight = 500),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),

            // Body Styles
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
                fontFamily = robotoFlex(weight = 300),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),

            // Label Styles
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
                fontFamily = roundedFont,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        )
    }

@Composable
private fun flexFontEmphasis(
    slant: Float = 0f,
    fontWeight: Int = 1000,
    fontWidth: Float = 120f,
): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.GoogleSansFlex,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(fontWeight),
                    FontVariation.slant(slant),
                    FontVariation.width(fontWidth),
                ),
        ),
    )

@Composable
private fun flexFontRounded(): FontFamily =
    FontFamily(
        Font(
            resource = Res.font.GoogleSansFlex,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(800),
                    FontVariation.Setting("ROND", 100f),
                ),
        ),
    )

@Composable
private fun robotoFlex(
    weight: Int = 400,
    slant: Float = 0f,
    width: Float = 100f
): FontFamily = FontFamily(
    Font(
        resource = Res.font.RobotoFlex,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.slant(slant),
            FontVariation.width(width),
        ),
    ),
)
