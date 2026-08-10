package com.sam.bluepad.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import com.sam.bluepad.platform.common_utils.PlatformFontProviderImpl

@OptIn(ExperimentalTextApi::class)
internal actual val SystemFontFamily: FontFamily?
    get() = PlatformFontProviderImpl().use { provider ->
        val font = provider.readFontFamily() ?: return@use null
        FontFamily(font)
    }
