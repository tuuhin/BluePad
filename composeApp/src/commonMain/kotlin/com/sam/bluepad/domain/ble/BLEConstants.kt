package com.sam.bluepad.domain.ble

import kotlin.uuid.Uuid

object BLEConstants {

	// device discovery
	val discoveryServiceId = Uuid.parse("d4764ea7-c3d1-426f-bf46-c96f4ee95aa8")
	val deviceInfoCharacteristics = Uuid.parse("049d551a-572a-49a4-b72b-feed8028336c")

	// device sync and proximity check
	val syncServiceId = Uuid.parse("a09b1351-a163-479e-ad1c-c860b6ad0f53")
	val deviceIdCharacteristics = Uuid.parse("049d551a-572a-49a4-b72b-feed8028336c")
	val connectionNonceCharacteristic = Uuid.parse("84ed9fb6-f86b-4ad3-8649-3ce94fdd9e88")
	val allowSyncCharacteristics = Uuid.parse("e8faba5c-5511-48e1-9810-1eb1f46dcaab")
	val receiverDeviceIdCharacteristics = Uuid.parse("320cba65-26b7-4f74-aafb-95e29c6f9649")
}