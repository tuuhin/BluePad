package com.sam.bluepad.utility.theme

expect class PlatformFontProviderImpl : IPlatformFontProvider {
    override fun readFontFamily(): String?
}
