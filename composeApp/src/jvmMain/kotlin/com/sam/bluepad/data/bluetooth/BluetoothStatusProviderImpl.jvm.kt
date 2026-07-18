package com.sam.bluepad.data.bluetooth

import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bt_common.platform.PlatformBTInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.thread

private const val TAG = "BluetoothStatusProvider"

@OptIn(ExperimentalAtomicApi::class)
actual class BluetoothStateProviderImpl : BluetoothStateProvider {

    override val isBtActive: Boolean
        get() = PlatformBTInfoProvider()
            .use { provider -> provider.isBluetoothActive() }

    override val bluetoothStatusFlow: Flow<Boolean>
        get() = callbackFlow {
            val provider = PlatformBTInfoProvider()
            provider.use { provider ->

                // initial bluetooth state
                launch(Dispatchers.IO) {
                    trySend(provider.isBluetoothActive())
                }

                Logger.d(tag = TAG) { "BLUETOOTH STATE SEND" }

                val handle = provider.registerCallback { state ->
                    trySend(state)
                    Logger.d(tag = TAG) { "BLUETOOTH STATE UPDATED" }
                }

                val isCleanUpDone = AtomicBoolean(false)

                fun cleanup() {
                    if (!isCleanUpDone.compareAndSet(expectedValue = false, newValue = true)) return
                    provider.unregisterCallback(handle)
                    Logger.d(tag = TAG) { "BLUETOOTH CALLBACK UNREGISTERED" }
                }

                val cleanUpThread = thread(false) {
                    cleanup()
                }

                // cleanup via runtime shutdown
                Runtime.getRuntime().addShutdownHook(cleanUpThread)

                awaitClose {
                    try {
                        // remove the shutdown hook
                        Runtime.getRuntime().removeShutdownHook(cleanUpThread)
                    } catch (_: IllegalStateException) {
                        Logger.w(tag = TAG) { "CLEANUP MANAGED BY RUNTIME STARTED " }
                    }
                    // cleanup by code
                    cleanup()
                }
            }
        }.flowOn(Dispatchers.IO)
}
