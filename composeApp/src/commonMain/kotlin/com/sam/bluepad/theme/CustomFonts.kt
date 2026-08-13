package com.sam.bluepad.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation

@Composable
expect fun systemFontFamily(): FontFamily?

@Composable
expect fun googleSansFlexFont(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily

@Composable
expect fun robotoFlexFont(settings: FontVariation.Settings = FontVariation.Settings()): FontFamily


