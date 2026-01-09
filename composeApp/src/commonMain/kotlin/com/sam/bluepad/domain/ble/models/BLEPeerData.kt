package com.sam.bluepad.domain.ble.models

import com.sam.bluepad.domain.models.DevicePlatformOS
import kotlin.uuid.Uuid

data class BLEPeerData(
	val deviceId: Uuid,
	val deviceName: String,
	val nonce: String? = null,
	val deviceOs: DevicePlatformOS? = null,
)