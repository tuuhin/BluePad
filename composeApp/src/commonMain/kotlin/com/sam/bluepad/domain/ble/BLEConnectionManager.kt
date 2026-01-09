package com.sam.bluepad.domain.ble

import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface BLEConnectionManager {

	val isDeviceConnected: Flow<BLEConnectionState>

	fun connectToDeviceAndRetrieveData(address: String): Flow<Resource<BLEPeerData, Exception>>

	fun disconnectAndClose()
}