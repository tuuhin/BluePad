package com.sam.bluepad.presentation.utils

import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.SketchModel
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

	val FAKE_SKETCH_MODEL = SketchModel(
		id = Uuid.random(),
		createdAt = LocalDateTime(2025, 1, 10, 4, 32),
		modifiedAt = LocalDateTime(2025, 1, 10, 4, 32),
		title = "How to play outswing",
		content = "Trent boult swinging the ball outside off stump and he hits a bouncer",
		contentHash = "9306d63f963638711dd2e78b17259abdb45df3ca8fb6063b4f51cdcce93cb16b",
	)
}