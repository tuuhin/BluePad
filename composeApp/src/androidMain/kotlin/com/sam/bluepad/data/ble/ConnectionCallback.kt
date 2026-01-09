package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

private const val TAG = "BLE_CONNECTION_CALLBACK"

@SuppressLint("MissingPermission")
class ConnectionCallback(
	private val onConnectionStateChange: (gatt: BluetoothGatt?, state: BLEConnectionState) -> Unit,
	private val onGAttFailed: (String) -> Unit,
	private val onReadCharacteristic: (BluetoothGatt, Uuid, ByteArray) -> Unit
) : BluetoothGattCallback() {

	private val _readQueue = ConcurrentLinkedQueue<BluetoothGattCharacteristic>()

	override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			onGAttFailed("Cannot read connection state")
			Logger.w(TAG) { "CONNECTION STATUS FAILED" }
			// clear the queue if anything fails
			_readQueue.clear()
			return
		}
		Logger.e(TAG) { "CONNECTION STATE CHANGED! :$newState" }
		when (newState) {
			BluetoothGatt.STATE_CONNECTED -> {
				onConnectionStateChange(gatt, BLEConnectionState.CONNECTED)
				//discover services
				gatt?.discoverServices()
			}

			BluetoothGatt.STATE_DISCONNECTED -> {
				// clear the queue if anything fails
				_readQueue.clear()
				onConnectionStateChange(gatt, BLEConnectionState.DISCONNECTING)
				gatt?.close()
			}

			BluetoothGatt.STATE_CONNECTING -> onConnectionStateChange(
				gatt,
				BLEConnectionState.CONNECTING
			)

			BluetoothGatt.STATE_DISCONNECTING -> onConnectionStateChange(
				gatt,
				BLEConnectionState.DISCONNECTING
			)

			else -> Logger.d(TAG) { "DEVICE STATE NOT CONNECTED : $newState" }
		}
	}

	override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			Logger.w(TAG) { "CANNOT DISCOVER SERVICES" }
			onGAttFailed("Cannot discover services")
			return
		}
		val service = gatt?.services
			?.find { it.uuid.toKotlinUuid() == BLEConstants.transportServiceId } ?: run {
			gatt?.disconnect()
			gatt?.close()
			Logger.w(TAG) { "INVALID CHARACTERISTICS FOUND CLOSING CONNECTION " }
			return
		}
		// we read all the characteristics
		val requiredCharacteristics = service.characteristics.filter { it.uuid != null }
		Logger.d(TAG) { "REQUIRED CHARACTERISTICS FOUND STARTING READ :${requiredCharacteristics.size}" }
		// read characteristic
		_readQueue.addAll(requiredCharacteristics)
		_readQueue.poll()?.let { gatt.readCharacteristic(it) }
	}

	override fun onCharacteristicRead(
		gatt: BluetoothGatt,
		characteristic: BluetoothGattCharacteristic,
		value: ByteArray,
		status: Int
	) {
		if (status != BluetoothGatt.GATT_SUCCESS) {
			onGAttFailed("Cannot read characteristics :${characteristic.uuid}")
			Logger.w(TAG) { "CANNOT READ CHARACTERISTICS" }
			return
		}
		val valueAsString = value.decodeToString()
		Logger.d(TAG) { "READING CHARACTERISTICS :${characteristic.uuid} :$valueAsString" }
		onReadCharacteristic(gatt, characteristic.uuid.toKotlinUuid(), value)
		// check if anything left on the queue
		_readQueue.poll()?.let { gatt.readCharacteristic(it) }
	}
}