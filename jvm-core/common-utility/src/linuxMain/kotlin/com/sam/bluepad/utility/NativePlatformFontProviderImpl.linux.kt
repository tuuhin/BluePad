package com.sam.bluepad.utility

import com.sam.bluepad.utility.domain.NativePlatformFontProvider
import kotlinx.cinterop.*
import platform.posix.pclose
import platform.posix.fgets
import platform.posix.popen

actual class NativePlatformFontProviderImpl : NativePlatformFontProvider {

    actual override fun readFontFamily(): String? {
        val command = "fc-match --format='%{family}' sans-serif"
        try {
            val pipe = popen(command, "r") ?: return null

            val buffer = ByteArray(128)
            var result = ""

            memScoped {
                val cBuffer = allocArray<ByteVar>(buffer.size)
                while (fgets(cBuffer, buffer.size, pipe) != null)
                    result += cBuffer.toKString()
            }
        } finally {
            pclose(pipe)
        }

        return result.trim().removeSurrounding("\"").takeIf { it.isNotEmpty() }
    }
}
