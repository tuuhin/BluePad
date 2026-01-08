package com.sam.bluepad.domain.ble.models

data class BLEPeerDevice(
	val deviceAddress: String,
	val bleDeviceName: String? = null,
	val rssi: Int
) {
	val signalStrength: BLEPeerSignalStrength
		get() = when {
			rssi > -50 -> BLEPeerSignalStrength.EXCELLENT
			rssi > -60 -> BLEPeerSignalStrength.GOOD
			rssi > -70 -> BLEPeerSignalStrength.AVG
			rssi > -80 -> BLEPeerSignalStrength.POOR
			rssi > -90 -> BLEPeerSignalStrength.VER_POOR
			else -> BLEPeerSignalStrength.UN_RELIABLE
		}
}