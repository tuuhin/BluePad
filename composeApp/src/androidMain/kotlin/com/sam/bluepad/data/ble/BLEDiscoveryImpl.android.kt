package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFilterNotNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.BuildKonfig
import com.sam.bluepad.data.utils.hasBLEScanPermission
import com.sam.bluepad.data.utils.hasCoarseLocationPermission
import com.sam.bluepad.data.utils.hasFineLocationPermission
import com.sam.bluepad.domain.ble.BLEConnectionType
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import com.sam.bluepad.domain.exceptions.BLEScanRunningException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.exceptions.LocationDisabledException
import com.sam.bluepad.domain.exceptions.LocationPermissionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.uuid.toJavaUuid

private const val TAG = "BLE_DISCOVERY"

@SuppressLint("MissingPermission")
actual class BLEDiscoveryImpl(private val context: Context) : BLEDiscoveryManager {

	private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }
	private val _locationManager by lazy { context.getSystemService<LocationManager>() }

	private val _peers = MutableStateFlow<List<BLEPeerDevice>>(emptyList())
	private val _isScanning = MutableStateFlow(false)

	override val scanResults: Flow<Set<BLEPeerDevice>>
		get() = _peers.map { it.toSet() }

	override val isScanning: Flow<Boolean>
		get() = _isScanning

	private val _btAdapter: BluetoothAdapter?
		get() = _bluetoothManager?.adapter

	private val isLocationActive: Boolean
		get() = _locationManager?.isLocationEnabled ?: false

	private val _bLeScanCallback = object : ScanCallback() {

		override fun onScanResult(callbackType: Int, result: ScanResult?) {
			val scanRecord = result?.scanRecord ?: return
			val device = result.device ?: return

			val parcelUid = ParcelUuid(BLEConstants.discoveryServiceId.toJavaUuid())
			val hasServiceId = (scanRecord.serviceUuids ?: emptyList()).contains(parcelUid)

			if (!hasServiceId) return

			val serviceData = scanRecord.getServiceData(parcelUid)
			Logger.d(TAG) { "SCAN RESULT FOUND ${scanRecord.serviceUuids} $serviceData" }

			// TODO: ONLY CHECKING THE SERVICE DATA FROM ANDROID S ,
			//  IDK ITS NOT WORKING ON LOWER API CHECK THIS LATER
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && serviceData?.decodeToString() != BuildKonfig.APP_ID) return


			val peerAddress = _peers.value.map { it.deviceAddress }
			if (device.address in peerAddress) {
				_peers.update { oldPeers ->
					oldPeers.fastMap { peer ->
						if (peer.deviceAddress == device.address)
							peer.copy(rssi = result.rssi)
						else peer
					}
				}
				Logger.d(TAG) { "DEVICE RSSI UPDATED :${device.address}" }
			} else {
				val updatedDevice = BLEPeerDevice(
					bleDeviceName = device.name,
					deviceAddress = device.address,
					rssi = result.rssi
				)
				_peers.update { oldPeers -> (oldPeers + updatedDevice).distinctBy { it.deviceAddress } }
				Logger.d(TAG) { "NEW DEVICE ADDED :${device.address}" }
			}
		}

		override fun onBatchScanResults(results: List<ScanResult?>?) {
			super.onBatchScanResults(results)

			val parcelUid = ParcelUuid(BLEConstants.discoveryServiceId.toJavaUuid())
			val peerAddress = _peers.value.map { it.deviceAddress }

			Logger.d(TAG) { "BATCHED SCAN RESULT FOUND" }

			val batchedResults = results?.fastFilterNotNull()
				// filter via service uuids
				?.fastFilter { result ->
					result.scanRecord?.serviceUuids?.contains(parcelUid) ?: false
				}
				// filter via service uuid content
				?.fastFilter { result ->
					result.scanRecord?.getServiceData(parcelUid)
						?.decodeToString() != BuildKonfig.APP_ID
				}
				?: return

			batchedResults.fastForEach { result ->
				if (result.device.address in peerAddress) {
					_peers.update { oldPeers ->
						oldPeers.fastMap { peer ->
							if (peer.deviceAddress == result.device.address)
								peer.copy(rssi = result.rssi)
							else peer
						}
					}
					Logger.d(TAG) { "DEVICE RSSI UPDATED :${result.device.address}" }
				} else {
					val updatedDevice = BLEPeerDevice(
						bleDeviceName = result.device.name,
						deviceAddress = result.device.address,
						rssi = result.rssi
					)
					_peers.update { oldPeers -> (oldPeers + updatedDevice).distinctBy { it.deviceAddress } }
					Logger.d(TAG) { "NEW DEVICE ADDED :${result.device.address}" }
				}
			}

			Logger.d(TAG) { "BATCHED RESULTS FOUND" }
		}

		override fun onScanFailed(errorCode: Int) {
			super.onScanFailed(errorCode)
			Logger.d(TAG) { "FAILED TO SEND ERROR CODE : $errorCode" }
		}
	}

	override suspend fun startScan(connection: BLEConnectionType, timeout: Duration)
			: Result<Unit> {
		// scan permission not found
		if (!context.hasBLEScanPermission)
			return Result.failure(BluetoothPermissionException())
		// location permission not found
		if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context.hasFineLocationPermission) && !context.hasCoarseLocationPermission)
			return Result.failure(LocationPermissionException())
		// bluetooth not enabled
		if (_bluetoothManager?.adapter?.isEnabled != true)
			return Result.failure(BluetoothNotEnabledException())
		// location not enabled
		if (!isLocationActive) return Result.failure(LocationDisabledException())
		// scan is running
		if (_isScanning.value) return Result.failure(BLEScanRunningException())

		// if normal scan is running then stop it
		if (_btAdapter?.isDiscovering == true) _btAdapter?.cancelDiscovery()

		return try {
			// starts the scan
			startScanCallBack()
			// the function suspends for breaktime
			withContext(Dispatchers.Main) {
				delay(timeout)
			}
			Result.success(Unit)
		} catch (e: Exception) {
			if (e is CancellationException) {
				Log.d(TAG, "SCAN CANCELLED")
				throw e
			}
			Log.d(TAG, "SOME ERROR OCCURRED", e)
			Result.failure(e)
		} finally {
			Log.d(TAG, "STOPING SCAN")
			// if exception is thrown or the try block executed this will be oke
			stopScanCallback()
		}
	}

	override suspend fun stopScanning() {
		if (_bluetoothManager?.adapter?.isEnabled != true) return
		if (!context.hasBLEScanPermission) return
		stopScanCallback()
	}

	override fun onClearScanResults() {
		_peers.update { emptyList() }
		Logger.d(TAG) { "PEER LIST CLEARED" }
	}

	private fun stopScanCallback() {
		// scan is not running so nothing to stop
		if (!_isScanning.value) return
		// stop the scan details
		_isScanning.update { false }
		_btAdapter?.bluetoothLeScanner?.stopScan(_bLeScanCallback)
		Logger.d(TAG) { "SCAN STOPPED" }
	}

	private fun startScanCallBack(connection: BLEConnectionType = BLEConnectionType.DEVICE_DISCOVERY) {
		if (_isScanning.value) return
		// stop classic scan if running
		if (_btAdapter?.isDiscovering == true) _btAdapter?.cancelDiscovery()

		val scanFilterUUId = when (connection) {
			BLEConnectionType.DEVICE_DISCOVERY -> BLEConstants.discoveryServiceId
			BLEConnectionType.PROXIMITY_AND_SYNC -> BLEConstants.syncServiceId
		}

		// updates is scanning
		_isScanning.update { true }
		val scanFilters = listOf<ScanFilter>(
			ScanFilter.Builder()
				.setServiceUuid(ParcelUuid(scanFilterUUId.toJavaUuid()))
				.build()
		)

		val scanSettings = ScanSettings.Builder()
			.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
			.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
			.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
			.setLegacy(false)
			.build()

		_btAdapter?.bluetoothLeScanner?.startScan(scanFilters, scanSettings, _bLeScanCallback)
		Logger.d(TAG) { "SCAN STARTED " }
	}
}