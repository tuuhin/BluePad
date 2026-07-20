package com.sam.bluepad.utility

import com.sam.bluepad.utility.domain.NativePlatformFontProvider

expect class NativePlatformFontProviderImpl : NativePlatformFontProvider {
    override fun readFontFamily(): String?
}
