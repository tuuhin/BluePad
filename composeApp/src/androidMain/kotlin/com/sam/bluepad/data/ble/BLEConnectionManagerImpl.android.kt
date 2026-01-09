package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.hasBLEScanPermission
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.exceptions.BLEConnectionFailedException
import com.sam.bluepad.domain.exceptions.BluetoothInvalidAddressException
import com.sam.bluepad.domain.exceptions.BluetoothInvalidDeviceException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlin.uuid.Uuid

private const val TAG = "BLE_CONNECTOR"

@SuppressLint("MissingPermission")
actual class BLEConnectionManagerImpl(private val context: Context) : BLEConnectionManager {

	private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

	private val _btAdapter: BluetoothAdapter?
		get() = _bluetoothManager?.adapter

	private val _connectionState = MutableStateFlow(BLEConnectionState.DISCONNECTED)
	override val isDeviceConnected: Flow<BLEConnectionState>
		get() = _connectionState.asStateFlow()

	private var _gattConnection: BluetoothGatt? = null


	override fun connectToDeviceAndRetrieveData(address: String): Flow<Resource<BLEPeerData, Exception>> {
		return callbackFlow {

			var deviceName: String? = null
			var deviceId: Uuid? = null

			var deviceNonce: String? = null
			var deviceOs: DevicePlatformOS? = null

			var isDeviceNonceRead = false
			var isDeviceOsRead = false

			trySend(Resource.Loading)

			// TODO: SOME OF THE EDGE CASES ARE NOT HANDLED
			val callback = ConnectionCallback(
				onConnectionStateChange = { _, state ->
					val updated = _connectionState.updateAndGet { state }
					if (updated == BLEConnectionState.DISCONNECTED) {
						// disconnect and close the connection
						disconnectAndClose()
						close()
					}
				},
				onGAttFailed = { message ->
					trySend(Resource.Error(BLEConnectionFailedException(message)))
					_connectionState.update { BLEConnectionState.DISCONNECTED }
					close()
				},
				onReadCharacteristic = { gatt, uuid, value ->
					val stringValue = value.decodeToString()
					Logger.d(TAG) { "READ UUID :$uuid" }
					when (uuid) {
						BLEConstants.deviceNameCharacteristic -> deviceName = stringValue
						BLEConstants.deviceNonceCharacteristic -> {
							isDeviceNonceRead = true
							deviceNonce = stringValue
						}

						BLEConstants.deviceIdCharacteristics -> {
							try {
								val deviceUUID = Uuid.fromByteArray(value)
								deviceId = deviceUUID
							} catch (e: Exception) {
								Logger.e(TAG, e) { "DEVICE ID READ ERROR" }
							}
						}

						BLEConstants.deviceOSCharacteristics -> {
							deviceOs = try {
								DevicePlatformOS.valueOf(stringValue.uppercase())
							} catch (_: Exception) {
								DevicePlatformOS.UNKNOWN
							}
							isDeviceOsRead = true
						}
					}
					// everything is read happy to close the connection
					if (deviceName != null && deviceId != null && isDeviceNonceRead && isDeviceOsRead) {
						val peerData = BLEPeerData(
							deviceName = deviceName,
							deviceId = deviceId,
							nonce = deviceNonce,
							deviceOs = deviceOs
						)
						Logger.d(TAG) { "PEER DATA :$peerData" }
						trySend(Resource.Success(peerData))
						gatt.disconnect()
						// close the channel
						close()
					}
				},
			)

			// initiate connection
			val result = connectAndWaitForExchange(address, callback)
			if (result.isFailure) {
				val exception = result.exceptionOrNull()
				if (exception is Exception) trySend(Resource.Error(exception))
				// close the flow
				close()
			} else {
				// stop the connection if collector scope is cancelled
				awaitClose { disconnectAndClose() }
			}
		}
	}


	private fun connectAndWaitForExchange(address: String, gattCallback: BluetoothGattCallback)
			: Result<Unit> {
		if (_bluetoothManager?.adapter?.isEnabled != true)
			return Result.failure(BluetoothNotEnabledException())
		if (!context.hasBLEScanPermission)
			return Result.failure(BluetoothPermissionException())

		if (!BluetoothAdapter.checkBluetoothAddress(address))
			return Result.failure(BluetoothInvalidAddressException())

		return try {
			val device = _btAdapter?.getRemoteDevice(address)
				?: return Result.failure(BluetoothInvalidDeviceException())
			// connect to the gatt server
			_gattConnection = device
				.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
			Logger.d(TAG) { "CLIENT GATT CONNECTION STARTED" }
			// return success if there is no error
			Result.success(Unit)
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override fun disconnectAndClose() {
		try {
			if (_gattConnection != null) {
				_gattConnection?.disconnect()
				_gattConnection?.close()
				Logger.d(TAG) { "GATT CLIENT DISCONNECTED AND CLOSED" }
			}
			_gattConnection = null
			_connectionState.update { BLEConnectionState.DISCONNECTED }
		} catch (e: Exception) {
			Logger.e(TAG, e) { "GATT CONNECTED FAILED TO CLOSE" }
		}
	}
}