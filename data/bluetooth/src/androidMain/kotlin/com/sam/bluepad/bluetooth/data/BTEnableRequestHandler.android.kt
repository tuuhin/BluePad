package com.sam.bluepad.bluetooth.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.bluetooth.IBTEnableRequestHandler
import org.koin.core.annotation.Single

private const val TAG = "BluetoothEnableRequest"

@Single(binds = [IBTEnableRequestHandler::class])
actual class BTEnableRequestHandler(
    private val context: Context
) : IBTEnableRequestHandler {


    private val bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

    actual override val canOpenSettingsToActivateBT: Boolean = true

    actual override val canRequestBTActive: Boolean = true

    actual override suspend fun requestActive(): Result<Unit> {
        return runCatching {
            Logger.d(tag = TAG) { "REQUEST BLUETOOTH ENABLE" }
            val isEnabled = bluetoothManager?.adapter?.isEnabled ?: false
            if (isEnabled) return@runCatching

            try {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Logger.e(tag = TAG, throwable = e) { "FAILED TO REQUEST ENABLE ON BLUETOOTH" }
            }
        }
    }

    actual override suspend fun onOpenSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "FAILED TO OPEN BLUETOOTH SETTINGS" }
        }
    }
}
