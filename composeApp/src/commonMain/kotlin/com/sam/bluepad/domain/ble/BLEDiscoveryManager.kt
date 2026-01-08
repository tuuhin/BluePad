package com.sam.bluepad.domain.ble

import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface BLEDiscoveryManager {

	val scanResults: Flow<Set<BLEPeerDevice>>

	val isScanning: Flow<Boolean>

	suspend fun startScan(timeout: Duration = 20.seconds): Result<Unit>

	suspend fun stopScanning()

}
