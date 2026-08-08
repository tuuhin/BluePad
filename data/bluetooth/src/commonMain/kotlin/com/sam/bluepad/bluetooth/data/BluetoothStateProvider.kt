package com.sam.bluepad.bluetooth.data

import com.sam.bluepad.domain.bluetooth.IBluetoothStateProvider
import kotlinx.coroutines.flow.Flow

internal expect class BluetoothStateProvider : IBluetoothStateProvider {


    override val bluetoothStatusFlow: Flow<Boolean>

    override suspend fun isBtActive(): Boolean
}
