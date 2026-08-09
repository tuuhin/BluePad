package com.sam.bluepad.common.utils

import com.sam.bluepad.common.models.PlatformOS
import org.koin.core.annotation.Factory


@Factory(binds = [IPlatformDataProvider::class])
actual class PlatformDataProvider : IPlatformDataProvider {


    actual override val platformOSVersion: String
        get() = System.getProperty("os.version", "")

    actual override val platformOS: PlatformOS
        get() {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("win") -> PlatformOS.WINDOWS
                os.contains("mac") -> PlatformOS.MACOS
                else -> throw UnsupportedOperationException("Unsupported operating system: $os")
            }
        }
}
