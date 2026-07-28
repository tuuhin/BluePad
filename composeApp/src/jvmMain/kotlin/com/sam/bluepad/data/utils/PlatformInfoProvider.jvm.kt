package com.sam.bluepad.data.utils

import com.sam.bluepad.domain.models.DevicePlatformOS

actual class PlatformInfoProvider {

	actual val platformOS: DevicePlatformOS
		get() {
			val os = System.getProperty("os.name").lowercase()
			return when {
				os.contains("win") -> DevicePlatformOS.WINDOWS
                os.contains("mac")-> DevicePlatformOS.MACOS
				else -> throw UnsupportedOperationException("Unsupported operating system: $os")
			}
		}

	actual val platformName: String
		get() = platformOS.name
}
