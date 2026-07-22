package com.sam.bluepad.domain.bluetooth

import kotlinx.coroutines.flow.Flow

interface BluetoothStateProvider {

    suspend fun isBtActive(): Boolean

	val bluetoothStatusFlow: Flow<Boolean>
}
