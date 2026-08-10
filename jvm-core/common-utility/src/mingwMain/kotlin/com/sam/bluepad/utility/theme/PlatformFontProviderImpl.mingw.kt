package com.sam.bluepad.utility.theme

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import platform.windows.NONCLIENTMETRICSW
import platform.windows.SPI_GETNONCLIENTMETRICS
import platform.windows.SystemParametersInfoW

actual class PlatformFontProviderImpl : IPlatformFontProvider {

    actual override fun readFontFamily(): String? {
        return memScoped {
            val ncm = alloc<NONCLIENTMETRICSW>().apply {
                cbSize = sizeOf<NONCLIENTMETRICSW>().toUInt()
            }
            val boolResult = SystemParametersInfoW(
                SPI_GETNONCLIENTMETRICS.toUInt(),
                sizeOf<NONCLIENTMETRICSW>().toUInt(),
                ncm.ptr, 0u,
            )
            if (boolResult == 0) return@memScoped null
            val fontNamePointer = ncm.lfMessageFont.lfFaceName
            return fontNamePointer.toKString()
        }
    }
}
