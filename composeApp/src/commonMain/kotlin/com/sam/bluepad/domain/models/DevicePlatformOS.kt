package com.sam.bluepad.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DevicePlatformOS {
	@SerialName("1")
	ANDROID,

	@SerialName("2")
	WINDOWS,

	@SerialName("3")
	UNKNOWN;

	companion object {
		fun fromString(value: String): DevicePlatformOS {
			return try {
				val upperCase = value.uppercase()
				DevicePlatformOS.valueOf(upperCase)
			} catch (_: IllegalStateException) {
				UNKNOWN
			}
		}
	}
}