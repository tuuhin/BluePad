package com.sam.bluepad.domain.ble

import com.sam.bluepad.domain.ble.models.BLEPeerData
import kotlinx.coroutines.flow.Flow

interface BLEAdvertisementManager {

	val isRunning: Flow<Boolean>

	val errorFlow: Flow<Exception>

	val peerSaveDevices: Flow<List<BLEPeerData>>

	suspend fun startAdvertising(type: BLEConnectionType = BLEConnectionType.DEVICE_DISCOVERY): Result<Unit>

	fun stopAdvertising()

	fun cleanUp()
}