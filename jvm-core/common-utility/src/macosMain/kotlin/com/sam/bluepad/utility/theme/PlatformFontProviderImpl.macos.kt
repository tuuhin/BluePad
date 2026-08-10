package com.sam.bluepad.utility.theme

import platform.AppKit.NSFont

actual class PlatformFontProviderImpl : IPlatformFontProvider {
    actual override fun readFontFamily(): String? {
        val font = NSFont.systemFontOfSize(NSFont.systemFontSize)
        return font.familyName
    }
}
