package com.sam.bluepad.domain.ble

import kotlin.uuid.Uuid

object BLEConstants {

	val transportServiceId = Uuid.parse("d4764ea7-c3d1-426f-bf46-c96f4ee95aa8")
	val transportServiceData = Uuid.parse("63938ac9-76ae-4a5e-9b3e-8a34dfc6d077")

	// device name and device nonce
	val deviceNameCharacteristic = Uuid.parse("7b7e0932-c27d-4729-be34-c1d188b1cc29")
	val deviceNonceCharacteristic = Uuid.parse("84ed9fb6-f86b-4ad3-8649-3ce94fdd9e88")
}