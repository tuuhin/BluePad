package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.PlatformInfoProvider
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
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

private const val TAG = "BLE_CONNECTOR"

@SuppressLint("MissingPermission")
actual class BLEConnectionManagerImpl(
	private val context: Context,
	private val protoBuf: ProtoBuf,
	private val deviceInfoProvider: LocalDeviceInfoProvider,
	private val platformInfoProvider: PlatformInfoProvider,
	private val randomGenerator: RandomGenerator,
) : BLEConnectionManager {

	private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

	private val _btAdapter: BluetoothAdapter?
		get() = _bluetoothManager?.adapter

	private val _connectionState = MutableStateFlow(BLEConnectionState.DISCONNECTED)
	override val connectionState: Flow<BLEConnectionState>
		get() = _connectionState.asStateFlow()

	private var _gattConnection: BluetoothGatt? = null


	override fun connectAndReceiveData(
		address: String,
		informReceiver: Boolean,
		disconnectOnDone: Boolean
	): Flow<Resource<BLEPeerData, Exception>> = callbackFlow {

		trySend(Resource.Loading)

		// TODO: SOME OF THE EDGE CASES ARE NOT HANDLED
		val callback = ConnectionCallback(
			onConnectionStateChange = { _, state ->
				val updated = _connectionState.updateAndGet { state }
				if (updated == BLEConnectionState.DISCONNECTED) {
					// disconnect and close the connection
					disconnect()
					close()
				}
			},
			onGAttFailed = { message ->
				trySend(Resource.Error(BLEConnectionFailedException(message)))
				_connectionState.update { BLEConnectionState.DISCONNECTED }
				close()
			},
			onReadCharacteristic = { gatt, uuid, bytes ->
				when (uuid) {
					BLEConstants.deviceInfoCharacteristics -> {
						val peerData = try {
							protoBuf.decodeFromByteArray<BLEPeerData>(bytes)
						} catch (e: Exception) {
							Logger.e(TAG, e) { "UNABLE TO DECODE BYTES" }
							return@ConnectionCallback
						}
						Logger.d(TAG) { "PEER DATA :$peerData" }
						trySend(Resource.Success(peerData))
						if (informReceiver) runBlocking {
							gatt.sendDeviceInfo(nonce = peerData.nonce)
						}
						if (disconnectOnDone) {
							gatt.disconnect()
							close()
						}
					}

					else -> {}
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
			awaitClose {
				callback.cleanUp()
				disconnect()
			}
		}
	}

	@Suppress("DEPRECATION")
	private suspend fun BluetoothGatt.sendDeviceInfo(nonce: String? = null) {
		val characteristics = services.find { it.uuid == BLEConstants.discoveryServiceId }
			?.characteristics
			?.find { it == BLEConstants.deviceInfoCharacteristics }
			?: return

		val info = deviceInfoProvider.readDeviceInfo.first()
		val peerData = BLEPeerData(
			deviceId = info.deviceId,
			deviceName = info.name,
			deviceOs = platformInfoProvider.platformOS,
			nonce = nonce
		)

		val bytes = try {
			protoBuf.encodeToByteArray<BLEPeerData>(peerData)
		} catch (e: Exception) {
			Logger.e(TAG, e) { "UNABLE TO SERIALIZE DEVICE INFO" }
			return
		}

		val isSuccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			writeCharacteristic(
				characteristics,
				bytes,
				BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
			) == BluetoothStatusCodes.SUCCESS
		} else {
			characteristics.value = bytes
			writeCharacteristic(characteristics)
		}
		Logger.d(TAG) { "SENDING CURRENT DEVICE INFO SUCCESS :$isSuccess" }
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

	override fun disconnect() {
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