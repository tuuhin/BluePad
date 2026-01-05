package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMap
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.hasBLEFeature
import com.sam.bluepad.data.utils.hasBLEScanPermission
import com.sam.bluepad.data.utils.hasLocationPermission
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLEDiscoveryManager
import com.sam.bluepad.domain.ble.BLEPeerDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val TAG = "BLE_DISCOVERY"

@SuppressLint("MissingPermission")
actual class BLEDiscoveryImpl(private val context: Context) : BLEDiscoveryManager {

	private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

	private val _peers = MutableStateFlow<List<BLEPeerDevice>>(emptyList())
	private val _isScanning = MutableStateFlow(false)

	override val isScanning: Flow<Boolean>
		get() = _isScanning

	override val scanResults: Flow<Set<BLEPeerDevice>>
		get() = _peers.map { peer -> peer.fastDistinctBy { it.deviceAddress }.toSet() }

	override val hasBLEFeature: Boolean
		get() = context.hasBLEFeature

	private var _gattConnection: BluetoothGatt? = null

	private val _btAdapter: BluetoothAdapter?
		get() = _bluetoothManager?.adapter

	private val _bLeScanCallback = object : ScanCallback() {

		override fun onScanResult(callbackType: Int, result: ScanResult?) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && result?.isConnectable == false) return
			val address = result?.device?.address ?: return
			val deviceAddresses = _peers.value.map { it.deviceAddress }
			if (address in deviceAddresses) return

			val serviceData = result.scanRecord
				?.getServiceData(ParcelUuid(BLEConstants.transportServiceId.toJavaUuid()))

			var newDevice = BLEPeerDevice(
				deviceAddress = result.device.address,
				deviceName = result.device.name,
				rssi = result.rssi,
				appId = serviceData?.let(Uuid::fromByteArray)
			)
			// add it to devices
			_peers.update { devices -> (devices + newDevice).fastDistinctBy { it.deviceAddress } }

			var isNameRead = false
			var isNonceRead = false

			val callback = ConnectionCallback { gatt, uuid, value ->
				when (uuid) {
					BLEConstants.deviceNameCharacteristic -> {
						val copy = newDevice.copy(deviceName = value)
						isNameRead = true
						_peers.update { oldPeers ->
							oldPeers.fastMap { device ->
								if (device.deviceAddress == newDevice.deviceAddress) copy
								else device
							}
						}
					}

					BLEConstants.deviceNonceCharacteristic -> {
						val copy = newDevice.copy(deviceNonce = value)
						isNonceRead = true
						_peers.update { oldPeers ->
							oldPeers.fastMap { device ->
								if (device.deviceAddress == newDevice.deviceAddress) copy
								else device
							}
						}
					}
				}
				if (isNameRead && isNonceRead) {
					Logger.i(TAG) { "CLOSING CONNECTION" }
					gatt.disconnect()
				}
			}
			// connect to the gatt
			_gattConnection?.close()
			_gattConnection = result.device.connectGatt(
				context,
				false,
				callback,
				BluetoothDevice.TRANSPORT_LE
			)
		}
	}

	override suspend fun startScan(timeout: Duration) {
		val result = checkIfPermissionAndBTEnabled()
		if (!result || _isScanning.value) return
		// if normal scan is running then stop it
		if (_btAdapter?.isDiscovering == true) _btAdapter?.cancelDiscovery()

		try {
			// starts the scan
			startScanCallBack()
			// the function suspends for breaktime
			withContext(Dispatchers.Main) {
				delay(timeout)
			}
		} catch (e: Exception) {
			if (e is CancellationException) {
				Log.d(TAG, "SCAN CANCELLED")
				throw e
			}
			e.printStackTrace()
		} finally {
			Log.d(TAG, "STOPING SCAN")
			// if exception is thrown or the try block executed this will be oke
			stopScanCallback()
		}
	}

	override suspend fun stopScanning() {
		val result = checkIfPermissionAndBTEnabled()
		if (!result) return
		stopScanCallback()
	}

	private fun stopScanCallback() {
		// scan is not running so nothing to stop
		if (!_isScanning.value) return
		// stop the scan details
		_isScanning.update { false }
		_btAdapter?.bluetoothLeScanner?.stopScan(_bLeScanCallback)
		Log.d(TAG, "SCAN STOPPED")
		// if gatt connection present clear that
		_gattConnection?.close()
		_gattConnection = null
		Logger.d(TAG) { "GATT CONNECTION CLOSED IF PRESENT" }
	}

	private fun startScanCallBack() {
		if (_isScanning.value) return
		// updates is scanning
		_isScanning.update { true }

		val scanFilters = listOf(
			ScanFilter.Builder()
				.setServiceUuid(ParcelUuid(BLEConstants.transportServiceId.toJavaUuid()))
				.build()
		)

		// stop classic scan if running
		if (_btAdapter?.isDiscovering == true) _btAdapter?.cancelDiscovery()
		val scanSettings = ScanSettings.Builder()
			.build()

		_btAdapter?.bluetoothLeScanner?.startScan(scanFilters, scanSettings, _bLeScanCallback)
		Log.d(TAG, "SCAN STARTED ")
	}


	private fun checkIfPermissionAndBTEnabled(): Boolean {
		val isBTEnabled = _btAdapter?.isEnabled ?: false
		val hasScanPermission: Boolean = context.hasBLEScanPermission
		val hasLocationPermission: Boolean = context.hasLocationPermission

		return hasBLEFeature && isBTEnabled && hasScanPermission && hasLocationPermission
	}
}