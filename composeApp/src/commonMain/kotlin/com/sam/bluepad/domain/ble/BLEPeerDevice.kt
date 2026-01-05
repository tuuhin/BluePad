package com.sam.bluepad.domain.ble

import kotlin.uuid.Uuid

data class BLEPeerDevice(
	val deviceAddress: String,
	val appId: Uuid? = null,
	val deviceName: String? = null,
	val deviceNonce: String? = null,
	val rssi: Int
)