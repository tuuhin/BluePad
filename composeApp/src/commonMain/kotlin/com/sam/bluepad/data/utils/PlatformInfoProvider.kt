package com.sam.bluepad.data.utils

import com.sam.bluepad.domain.models.DevicePlatformOS

expect class PlatformInfoProvider {

	val platformOS: DevicePlatformOS
	val platformName: String
}

