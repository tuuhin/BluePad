package com.sam.bluepad.domain.ble

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface BLEDiscoveryManager {

	val scanResults: Flow<Set<BLEPeerDevice>>

	val hasBLEFeature: Boolean

	val isScanning: Flow<Boolean>

	suspend fun startScan(timeout: Duration = 10.seconds)

	suspend fun stopScanning()

}
