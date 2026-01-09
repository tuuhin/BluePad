package com.sam.bluepad.domain.utils

actual class PlatformInfoProvider {
	actual fun platformName(): String = System.getProperty("os.name").uppercase()
}