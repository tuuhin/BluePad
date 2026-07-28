package com.sam.bluepad.utility

import com.sam.bluepad.utility.domain.NativePlatformFontProvider
import platform.AppKit.NSFont

actual class NativePlatformFontProviderImpl : NativePlatformFontProvider {

    actual override fun readFontFamily(): String? {
        val font = NSFont.systemFontOfSize(NSFont.systemFontSize)
        return font.familyName
    }
}
