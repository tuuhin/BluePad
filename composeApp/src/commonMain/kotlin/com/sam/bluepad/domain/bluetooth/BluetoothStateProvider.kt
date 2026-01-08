package com.sam.bluepad.domain.bluetooth

import kotlinx.coroutines.flow.Flow

interface BluetoothStateProvider {

	val isBtActive: Boolean

	val bluetoothStatusFlow: Flow<Boolean>
}