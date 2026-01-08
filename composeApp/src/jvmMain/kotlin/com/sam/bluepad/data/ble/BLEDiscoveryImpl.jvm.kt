package com.sam.bluepad.data.ble

import androidx.compose.ui.util.fastMap
import co.touchlab.kermit.Logger
import com.juul.kable.Advertisement
import com.juul.kable.Scanner
import com.juul.kable.logs.Logging
import com.sam.ble_common.BluetoothInfoProvider
import com.sam.blejavaadvertise.BLEAdvertiser
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BLEScanRunningException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.use_cases.AppHasher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

private const val TAG = "BLE_SCAN_DEVICES"

actual class BLEDiscoveryImpl(
	private val hasher: AppHasher
) : BLEDiscoveryManager {

	private val _isStopLock = Mutex()

	private val _peers = MutableStateFlow<List<BLEPeerDevice>>(emptyList())
	private val _isScanning = MutableStateFlow(false)

	override val scanResults: Flow<Set<BLEPeerDevice>>
		get() = _peers.map { it.toSet() }

	override val isScanning: Flow<Boolean>
		get() = _isScanning

	private val _scanner = Scanner {
		logging {
			this.format = Logging.Format.Compact
			this.level = Logging.Level.Events
		}

		filters {
			match {
//				services = listOf(BLEConstants.baseAdvertisementId)
			}
		}
	}

	private var scanJob: Job? = null

	override suspend fun startScan(timeout: Duration): Result<Unit> {

		if (!BluetoothInfoProvider.isBluetoothActive())
			return Result.failure(BluetoothNotEnabledException())

		if (!BLEAdvertiser.nativeIsLeSecureConnectionAvailable())
			return Result.failure(BLENotSupportedException())

		if (_isScanning.value) return Result.failure(BLEScanRunningException())

		return try {
			coroutineScope {
				scanJob = launch(Dispatchers.IO) {
					try {
						withTimeout(timeout) {
							_scanner.advertisements
								.onStart { onStartAdvertisements() }
								.onCompletion { onStopAdvertisements() }
								.collect(::handleAdvertisement)
						}
					} catch (_: TimeoutCancellationException) {
						Logger.d(TAG) { "SCAN TIMEOUT" }
					}
				}
				Logger.d(TAG) { "ADDING SCAN JOB" }
				scanJob?.join()
			}
			Result.success(Unit)
		} catch (e: CancellationException) {
			Logger.d(TAG) { "SCAN CANCELLED" }
			throw e
		} catch (e: Exception) {
			Logger.e(TAG, e) { "SOME EXCEPTION OCCURRED" }
			Result.failure(e)
		} finally {
			stopScanning()
		}
	}

	override suspend fun stopScanning() {
		_isStopLock.withLock {
			if (scanJob?.isActive == true) {
				Logger.d(TAG) { " SCAN JOB WAS ACTIVE CANCELLING IT" }
				scanJob?.cancel()
				scanJob = null
			}
			_isScanning.value = false
		}
	}

	private fun handleAdvertisement(advertisement: Advertisement) {
		// only capture connective advertisements
		if (advertisement.uuids.isEmpty()) return

		// TODO: FILTERING VIA SERVICE DATA SHOULD BE WORKED BUT NOT SO LETS CONTINUE
		// WITH JUST THE TRANSPORT SERVICE ID MATCH

//		val appNameBytes = BuildKonfig.APP_ID.encodeToByteArray()
//		if (!serviceData.contentEquals(appNameBytes)) return

		val result = advertisement.uuids.contains(BLEConstants.transportServiceId)
		if (!result) return

		val address = advertisement.identifier.toString()
		val peerAddress = _peers.value.map { it.deviceAddress }
		if (address in peerAddress) {
			_peers.update { oldPeers ->
				oldPeers.fastMap { peer ->
					if (peer.deviceAddress == address) peer.copy(rssi = advertisement.rssi)
					else peer
				}
			}
			Logger.d(TAG) { "DEVICE RSSI UPDATED :${address}" }
		} else {
			advertisement.peripheralName
			val updatedDevice = BLEPeerDevice(
				bleDeviceName = advertisement.peripheralName,
				deviceAddress = advertisement.identifier.toString(),
				rssi = advertisement.rssi
			)
			_peers.update { oldPeers -> (oldPeers + updatedDevice).distinctBy { it.deviceAddress } }
			Logger.i(TAG) { "NEW DEVICE ADDED :${address}" }
		}
	}

	private fun onStartAdvertisements() {
		Logger.i(TAG) { "JVM BLE SCAN STARTED" }
		_isScanning.update { true }
	}

	private fun onStopAdvertisements() {
		Logger.i(TAG) { "JVM BLE SCAN STOPPED" }
		_isScanning.update { false }
	}
}