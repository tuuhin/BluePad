package com.sam.bluepad.domain.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

data class ExternalDeviceModel(
	val id: Uuid,
	val displayName: String? = null,
	val pairedAt: LocalDateTime? = null,
	val lastSeenAt: LocalDateTime? = null,
	val deviceOs: DevicePlatformOS = DevicePlatformOS.UNKNOWN
)