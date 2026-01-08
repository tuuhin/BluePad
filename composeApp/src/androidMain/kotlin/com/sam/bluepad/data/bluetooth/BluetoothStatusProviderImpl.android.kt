package com.sam.bluepad.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class BluetoothStateProviderImpl(private val context: Context) : BluetoothStateProvider {

	private val _btManager by lazy { context.getSystemService<BluetoothManager>() }

	override val isBtActive: Boolean
		get() = _btManager?.adapter?.isEnabled == true

	override val bluetoothStatusFlow: Flow<Boolean>
		get() = callbackFlow {

			trySend(isBtActive)

			val receiver = object : BroadcastReceiver() {
				override fun onReceive(context: Context?, intent: Intent?) {
					if (intent == null || intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
					val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
					trySend(btState == BluetoothAdapter.STATE_ON)
				}
			}

			val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

			ContextCompat.registerReceiver(
				context,
				receiver,
				intentFilter,
				ContextCompat.RECEIVER_EXPORTED
			)
			awaitClose {
				context.unregisterReceiver(receiver)
			}
		}
}