package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.juul.kable.NotConnectedException
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.logs.Logging
import com.juul.kable.logs.SystemLogEngine
import com.juul.kable.toIdentifier
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.exceptions.BLEConnectionFailedException
import com.sam.bluepad.domain.exceptions.BLEServiceNotFoundException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val TAG = "BLE_CONNECTOR"

actual class BLEConnectionManagerImpl(
	private val protoBuf: ProtoBuf,
	private val deviceInfoProvider: LocalDeviceInfoProvider,
	private val platformInfoProvider: PlatformInfoProvider,
	private val randomGenerator: RandomGenerator,
) : BLEConnectionManager {

	@Volatile
	private var _peripheral: Peripheral? = null

	private val _deviceConnectionState = MutableStateFlow(BLEConnectionState.DISCONNECTED)
	override val connectionState: Flow<BLEConnectionState>
		get() = _deviceConnectionState.asStateFlow()

	override fun connectAndReceiveData(
		address: String,
		informReceiver: Boolean,
		disconnectOnDone: Boolean
	): Flow<Resource<BLEPeerData, Exception>> = callbackFlow {

		trySend(Resource.Loading)

		// reset the peripheral
		cleanUp()
		// create the peripheral
		val peripheral = try {
			createAndConnect(address).also { _peripheral = it }
		} catch (e: Exception) {
			Logger.w(TAG, e) { "CANNOT CREATE THE PERIPHERAL DEVICES" }
			trySend(Resource.Error(e))
			close()
			return@callbackFlow
		}
		// read the service and characteristics
		peripheral.onReadDiscoveryCharacteristics(
			onServiceNotFound = { trySend(Resource.Error(BLEServiceNotFoundException())) },
			onCharacteristicData = { peri, result ->
				val peerData = result
					.getOrDefault(BLEConstants.deviceInfoCharacteristics, null)
					?.let { bytes ->
						try {
							protoBuf.decodeFromByteArray<BLEPeerData>(bytes)
						} catch (e: Exception) {
							Logger.e(TAG, e) { "UNABLE TO DECODE BYTES" }
							null
						}
					} ?: return@onReadDiscoveryCharacteristics

				Logger.d(TAG) { "PEER DATA READ :$peerData" }
				trySend(Resource.Success(peerData))

				if (informReceiver) peri.sendCurrentDeviceInfo(nonce = peerData.nonce)
				if (disconnectOnDone) {
					Logger.d(TAG) { "DISCONNECTING CONNECTION" }
					peri.disconnect()
				}
			},
		)
		// clean the peripheral when done
		awaitClose { cleanUp() }
	}.catch { err ->
		if (err is CancellationException) Logger.d(TAG) { "CANCELLATION EXCEPTION OCCURRED" }
		if (err is Exception) emit(Resource.Error(err))
	}

	private fun Peripheral.observePeripheralState() {
		state.onEach { state ->
			when (state) {
				is State.Connected -> _deviceConnectionState.update { BLEConnectionState.CONNECTED }
				State.Connecting.Bluetooth, State.Connecting.Services -> _deviceConnectionState.update { BLEConnectionState.CONNECTING }
				State.Disconnecting -> _deviceConnectionState.update { BLEConnectionState.DISCONNECTING }
				is State.Disconnected -> _deviceConnectionState.update { BLEConnectionState.DISCONNECTED }
				else -> {}
			}
			Logger.d(TAG) { "PERIPHERAL CONNECTION STATE :$state" }
		}.launchIn(scope)
	}

	private suspend fun Peripheral.onReadDiscoveryCharacteristics(
		onServiceNotFound: suspend () -> Unit = {},
		onCharacteristicData: suspend (Peripheral, Map<Uuid, ByteArray>) -> Unit,
	) {
		state.onEach { state ->
			if (state !is State.Connected) return@onEach

			val services = services.value
				?.find { it.serviceUuid == BLEConstants.discoveryServiceId }

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
						val data = this@onReadDiscoveryCharacteristics.read(characteristic)
						id to data
					}
				}
				val result = deferredResults.awaitAll()
				onCharacteristicData(this@onReadDiscoveryCharacteristics, result.toMap())
			}
		}.launchIn(scope)
	}

	private suspend fun Peripheral.sendCurrentDeviceInfo(nonce: String? = null) {
		val characteristic = services.value
			?.find { it.serviceUuid == BLEConstants.discoveryServiceId }
			?.characteristics
			?.find { it.characteristicUuid == BLEConstants.deviceInfoCharacteristics } ?: return

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

		write(
			characteristic = characteristic,
			data = bytes,
			writeType = WriteType.WithoutResponse
		)
		Logger.d(TAG) { "SEND WRITE INFO TO ${characteristic.characteristicUuid} LENGTH :${bytes.size}" }
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
		peripheral.observePeripheralState()
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

	override suspend fun disconnect() {
		try {
			_peripheral?.disconnect()
			Logger.d(TAG) { "PERIPHERAL DISCONNECTED" }
			_deviceConnectionState.update { BLEConnectionState.DISCONNECTED }
		} catch (e: Exception) {
			Logger.e(TAG, e) { "FAILED TO CLOSE THE CONNECTION" }
		}
	}

	override fun cleanUp() {
		try {
			if (_peripheral != null) {
				_peripheral?.close()
				_peripheral = null
				Logger.d(TAG) { "PERIPHERAL CONNECTION CLOSED" }
			}
		} catch (e: Exception) {
			Logger.e(TAG, e) { "FAILED TO CLOSE THE CONNECTION" }
		}
	}
}