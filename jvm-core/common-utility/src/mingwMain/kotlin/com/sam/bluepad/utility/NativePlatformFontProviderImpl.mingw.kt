package com.sam.bluepad.utility

import com.sam.bluepad.utility.domain.NativePlatformFontProvider
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import platform.windows.NONCLIENTMETRICSW
import platform.windows.SPI_GETNONCLIENTMETRICS
import platform.windows.SystemParametersInfoW

actual class NativePlatformFontProviderImpl : NativePlatformFontProvider {

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
