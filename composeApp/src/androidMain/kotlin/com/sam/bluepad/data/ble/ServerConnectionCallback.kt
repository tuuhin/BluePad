package com.sam.bluepad.data.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.content.Context
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import java.security.SecureRandom
import kotlin.uuid.toJavaUuid

private const val TAG = "ServerConnectionCallback"

private typealias SendResponse = (device: BluetoothDevice, requestId: Int, status: Int, offset: Int, value: ByteArray?) -> Unit

class ServerConnectionCallback(private val context: Context) : BluetoothGattServerCallback() {

	private val _random by lazy { SecureRandom() }

	private var _sendResponse: SendResponse? = null
	private var _onServiceAdded: (() -> Unit)? = null

	fun setOnSendResponse(callback: SendResponse) {
		_sendResponse = callback
	}

	fun setOnServiceAdded(onServiceAdded: () -> Unit = {}) {
		_onServiceAdded = onServiceAdded
	}

	override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Logger.w(TAG) { "SOME ERROR IN ADDING THE SERVICE: $status" }
			return
		}
		if (service == null) return
		Logger.i(TAG) { "SERVICE :${service.uuid} ADDED" }

		_onServiceAdded?.invoke()
	}

	override fun onCharacteristicReadRequest(
		device: BluetoothDevice?,
		requestId: Int,
		offset: Int,
		characteristic: BluetoothGattCharacteristic?
	) {
		if (device == null || characteristic == null) return
		Logger.i(TAG) { "READ REQUESTED FOR CHARACTERISTICS :${characteristic.uuid}" }

		val charset = Charsets.UTF_8

		if (characteristic.service.uuid != BLEConstants.transportServiceId) return

		val deviceName = "Sam-Boult"
		val bytes = ByteArray(16)
		_random.nextBytes(bytes)

		val value = when (characteristic.uuid) {
			BLEConstants.deviceNameCharacteristic.toJavaUuid() -> deviceName.toByteArray(charset)
			BLEConstants.deviceNonceCharacteristic.toJavaUuid() -> bytes
			else -> null
		}
		val isSuccess = if (value == null) BluetoothGatt.GATT_FAILURE
		else BluetoothGatt.GATT_SUCCESS
		_sendResponse?.invoke(device, requestId, isSuccess, offset, value)
	}

	fun cleanUp() = Unit
}