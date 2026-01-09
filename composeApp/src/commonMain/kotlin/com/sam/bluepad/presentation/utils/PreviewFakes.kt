package com.sam.bluepad.presentation.utils

import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

object PreviewFakes {

	val FAKE_EXTERNAL_MODEL = ExternalDeviceModel(
		id = Uuid.random(),
		displayName = "new-balance",
		pairedAt = LocalDateTime(2025, 1, 10, 4, 32),
		lastSeenAt = LocalDateTime(2025, 1, 10, 4, 32),
		deviceOs = DevicePlatformOS.ANDROID
	)
}