package com.sam.bluepad.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.bluetooth.BTEnableRequestProvider

private const val TAG = "BluetoothEnableRequest"

actual class BTEnableRequestProviderImpl(private val context: Context) : BTEnableRequestProvider {

    private val _btManager by lazy { context.getSystemService<BluetoothManager>() }

    @SuppressLint("MissingPermission")
    override suspend fun invoke(): Result<Unit> {
        return runCatching {
            Logger.d(tag = TAG) { "REQUEST BLUETOOTH ENABLE" }
            val isEnabled = _btManager?.adapter?.isEnabled ?: false
            if (isEnabled) return@runCatching

            try {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Logger.e(tag = TAG, throwable = e) { "FAILED TO REQUEST INTENT" }
            }
        }
    }
}
