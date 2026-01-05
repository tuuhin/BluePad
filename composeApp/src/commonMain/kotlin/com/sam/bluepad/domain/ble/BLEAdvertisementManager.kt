package com.sam.bluepad.domain.ble

import kotlinx.coroutines.flow.Flow

interface BLEAdvertisementManager {

	val isRunning: Flow<Boolean>

	fun startAdvertising(deviceName: String, nonce: String? = null)

	fun stopAdvertising()

	fun cleanUp()
}