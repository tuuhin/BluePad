package com.sam.bluepad.data.bluetooth

import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bt_common.platform.PlatformBTInfoProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "BluetoothStatusProvider"

actual class BluetoothStateProviderImpl : BluetoothStateProvider {

    override val isBtActive: Boolean
        get() = PlatformBTInfoProvider()
            .use { provider -> provider.isBluetoothActive() }

    override val bluetoothStatusFlow: Flow<Boolean>
        get() = callbackFlow {
            val provider = PlatformBTInfoProvider()
            provider.use { provider ->
                trySend(provider.isBluetoothActive())
                Logger.d(tag = TAG) { "BLUETOOTH STATE SEND" }

                val handle = provider.registerCallback { state ->
                    trySend(state)
                    Logger.d(tag = TAG) { "BLUETOOTH STATE UPDATED" }
                }
                awaitClose {
                    provider.unregisterCallback(handle)
                    Logger.d(tag = TAG) { "BLUETOOTH CALLBACK UNREGISTERED" }
                }
            }
        }
}
