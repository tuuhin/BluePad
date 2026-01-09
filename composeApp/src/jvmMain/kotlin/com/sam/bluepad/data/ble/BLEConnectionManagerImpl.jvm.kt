package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.juul.kable.ExperimentalApi
import com.juul.kable.NotConnectedException
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.juul.kable.toIdentifier
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.exceptions.BLEConnectionFailedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val TAG = "BLE_CONNECTOR"

@OptIn(ExperimentalApi::class)
actual class BLEConnectionManagerImpl : BLEConnectionManager {

	private var _peripheral: Peripheral? = null

	private val _isDeviceConnected = MutableStateFlow(BLEConnectionState.DISCONNECTED)
	override val isDeviceConnected: Flow<BLEConnectionState>
		get() = _isDeviceConnected.asStateFlow()

	override fun connectToDeviceAndRetrieveData(address: String): Flow<Resource<BLEPeerData, Exception>> {
		return callbackFlow {
			trySend(Resource.Loading)
			// reset the value
			// now start
			try {
				val peripheral = createAndConnect(address).also { _peripheral = it }
				// read the service and characteristics
				peripheral.readServiceCharacteristics(
					disconnectAfterRead = false,
					onServiceNotFound = {},
					onCharacteristicData = { mapResult ->

						var name: String? = null
						var deviceId: Uuid? = null
						var nonce: String? = null
						var deviceOs: DevicePlatformOS? = null

						if (mapResult.contains(BLEConstants.deviceNameCharacteristic)) {
							val data = mapResult[BLEConstants.deviceNameCharacteristic]
							if (data != null) {
								val stringValue = data.decodeToString()
								name = stringValue
							}
						}
						if (mapResult.contains(BLEConstants.deviceIdCharacteristics)) {
							val data = mapResult[BLEConstants.deviceIdCharacteristics]
							if (data != null) {
								val uuid = Uuid.fromByteArray(data)
								deviceId = uuid
							}
						}
						if (mapResult.contains(BLEConstants.deviceNonceCharacteristic)) {
							val data = mapResult[BLEConstants.deviceNonceCharacteristic]
							if (data != null) {
								val stringValue = data.decodeToString()
								nonce = stringValue
							}
						}
						if (mapResult.contains(BLEConstants.deviceOSCharacteristics)) {
							val data = mapResult[BLEConstants.deviceOSCharacteristics]
							if (data != null) {
								val osValue = data.decodeToString().uppercase()
								deviceOs = try {
									DevicePlatformOS.valueOf(osValue)
								} catch (_: Exception) {
									DevicePlatformOS.UNKNOWN
								}
							}
						}
						if (name != null && deviceId != null) {
							val peerData = BLEPeerData(deviceId, name, nonce, deviceOs)
							Logger.d(TAG) { "PEER DATA :$peerData" }
							trySend(Resource.Success(peerData))
						}
					},
				)
				// disconnect when the flow is closed
				awaitClose { disconnectAndClose() }
			} catch (e: Exception) {
				trySend(Resource.Error(e))
				close()
			}
		}
	}

	private fun Peripheral.observePeripheralState(scope: CoroutineScope) {
		state.onEach { state ->
			when (state) {
				is State.Connected -> _isDeviceConnected.update { BLEConnectionState.CONNECTED }
				State.Connecting.Bluetooth, State.Connecting.Services -> _isDeviceConnected.update { BLEConnectionState.CONNECTING }
				State.Disconnecting -> _isDeviceConnected.update { BLEConnectionState.DISCONNECTING }
				is State.Disconnected -> _isDeviceConnected.update { BLEConnectionState.DISCONNECTED }
				else -> {}
			}
			Logger.d(TAG) { "PERIPHERAL CONNECTION STATE :$state" }
		}.launchIn(scope)
	}

	private suspend fun Peripheral.readServiceCharacteristics(
		disconnectAfterRead: Boolean = true,
		onServiceNotFound: suspend () -> Unit = {},
		onCharacteristicData: (Map<Uuid, ByteArray>) -> Unit = {}
	) {
		state.onEach { state ->
			if (state !is State.Connected) return@onEach

			val services = services.value
				?.find { it.serviceUuid == BLEConstants.transportServiceId }

			if (services == null) {
				Logger.w(TAG) { "REQUIRED SERVICE NOT FOUND" }
				onServiceNotFound()
				return@onEach
			}

			Logger.d(TAG) { "SERVICE FOUND CHARACTERISTICS :${services.characteristics.map { it.characteristicUuid }}" }

			scope.launch {
				val deferredResults = services.characteristics.map { characteristic ->
					async {
						val id = characteristic.characteristicUuid
						val data = this@readServiceCharacteristics.read(characteristic)
						id to data
					}
				}
				val result = deferredResults.awaitAll()
				onCharacteristicData(result.toMap())
				if (disconnectAfterRead) {
					Logger.d(TAG) { "DISCONNECTING CONNECTION" }
					disconnect()
				}
			}
		}.launchIn(scope)
	}

	suspend fun createAndConnect(address: String): Peripheral {
		val peripheral = Peripheral(address.toIdentifier()) {
			this.disconnectTimeout = 10.seconds
			this.forceCharacteristicEqualityByUuid = true
			// logging
			logging {
				identifier = "IDENTIFIER: $address"
				engine = SystemLogEngine
				level = Logging.Level.Data
			}
			// exception handler
			observationExceptionHandler { exp ->
				Logger.e(TAG, exp) { "EXCEPTION HAPPENED" }
			}
			onServicesDiscovered {
				Logger.d(TAG) { "SERVICE DISCOVERED SERVICE" }
			}
		}
		// reading the state of the peripheral
		peripheral.observePeripheralState(peripheral.scope)
		Logger.d(TAG) { "PERIPHERAL CONFIGURED" }
		try {
			peripheral.connect()
			Logger.d(TAG) { "PERIPHERAL ESTABLISHED" }
			return peripheral
		} catch (_: IllegalStateException) {
			throw BluetoothNotEnabledException()
		} catch (e: NotConnectedException) {
			throw BLEConnectionFailedException(e.message ?: "Connection Failed")
		}
	}

	override fun disconnectAndClose() {
		try {
			if (_peripheral != null) {
				_peripheral?.close()
				_peripheral = null
				Logger.d(TAG) { "PERIPHERAL CONNECTION CLOSED" }
			}
			_isDeviceConnected.update { BLEConnectionState.DISCONNECTED }
		} catch (_: CancellationException) {
			Logger.w(TAG) { "CONNECTION WAS CANCELLED" }
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}