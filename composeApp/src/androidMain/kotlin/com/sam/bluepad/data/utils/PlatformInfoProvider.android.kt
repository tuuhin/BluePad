package com.sam.bluepad.data.utils

import com.sam.bluepad.domain.models.DevicePlatformOS

actual class PlatformInfoProvider {

	actual val platformOS: DevicePlatformOS
		get() = DevicePlatformOS.ANDROID

	actual val platformName
		get() = platformOS.name
}