package com.sam.bluepad.domain.bluetooth

import kotlinx.coroutines.flow.Flow

interface IBluetoothStateProvider {


    suspend fun isBtActive(): Boolean

    val bluetoothStatusFlow: Flow<Boolean>
}
