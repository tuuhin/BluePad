package com.sam.bluepad.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import com.sam.bluepad.platform.common_utils.NativePlatformFontProviderImpl

@OptIn(ExperimentalTextApi::class)
internal actual val SystemFontFamily: FontFamily
    get() = NativePlatformFontProviderImpl().use { provider ->
        val font = provider.readFontFamily() ?: return@use FontFamily.Default
        FontFamily(font)
    }
