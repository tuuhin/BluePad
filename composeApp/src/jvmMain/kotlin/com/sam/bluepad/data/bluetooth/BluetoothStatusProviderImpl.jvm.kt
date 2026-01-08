package com.sam.bluepad.data.bluetooth

import co.touchlab.kermit.Logger
import com.sam.ble_common.BluetoothInfoProvider
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "BluetoothStatusProvider"

actual class BluetoothStateProviderImpl : BluetoothStateProvider {

	private val provider by lazy { BluetoothInfoProvider() }

	override val isBtActive: Boolean
		get() = provider.bluetoothStatus

	override val bluetoothStatusFlow: Flow<Boolean>
		get() = callbackFlow {
			trySend(provider.bluetoothStatus)
			Logger.d(TAG) { "BLUETOOTH STATE SEND" }

			provider.registerCallback { state ->
				trySend(state)
				Logger.d(TAG) { "BLUETOOTH STATE UPDATED" }
			}
			awaitClose {
				provider.unregisterCallback()
				Logger.d(TAG) { "BLUETOOTH CALLBACK UNREGISTERED" }
			}
		}
}