package com.sam.bluepad.domain.ble.models

import kotlin.uuid.Uuid

data class BLEPeerData(
	val deviceId: Uuid? = null,
	val deviceName: String? = null,
	val nonce: String? = null,
)