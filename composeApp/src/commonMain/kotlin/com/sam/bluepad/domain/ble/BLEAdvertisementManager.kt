package com.sam.bluepad.domain.ble

import kotlinx.coroutines.flow.Flow

interface BLEAdvertisementManager {

	val isRunning: Flow<Boolean>

	val errorFlow: Flow<Exception>

	suspend fun startAdvertising(type: BLEAdvertisementType = BLEAdvertisementType.DISCOVERY): Result<Unit>

	fun stopAdvertising()

	fun cleanUp()
}