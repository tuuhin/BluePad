package com.sam.bluepad.domain.utils

actual class PlatformInfoProvider {
	actual fun platformName(): String {
		val os = System.getProperty("os.name").lowercase()
		return when {
			os.contains("windows") -> "windows"
			os.contains("linux") -> "linux"
			os.contains("mac") -> "macos"
			else -> throw UnsupportedOperationException("Unsupported operating system: $os")
		}
	}
}