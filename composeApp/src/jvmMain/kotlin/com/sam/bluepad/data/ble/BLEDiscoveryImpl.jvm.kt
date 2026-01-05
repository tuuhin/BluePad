package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.ble_common.BluetoothUUID
import com.sam.ble_common.Service
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
import org.simplejavable.Adapter
import org.simplejavable.Peripheral
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.uuid.Uuid

private const val TAG = "BLEDiscoveryImpl.jvm"

actual class BLEDiscoveryImpl : BLEDiscoveryManager {

	private val _peers = MutableStateFlow<List<BLEPeerDevice>>(emptyList())
	private val _isScanning = MutableStateFlow(false)

	private val _btAdapter: Adapter? by lazy {
		Adapter.getAdapters().firstOrNull()?.apply { setEventListener(_eventListener) }
	}

	override val scanResults: Flow<Set<BLEPeerDevice>>
		get() = _peers.map { peer -> peer.distinctBy { it.deviceAddress }.toSet() }

	override val hasBLEFeature: Boolean
		get() = true

	override val isScanning: Flow<Boolean>
		get() = _isScanning

	private val _eventListener = object : Adapter.EventListener {

		override fun onScanStart() {
			Logger.i(TAG) { "SCAN STARTED!!" }
			_isScanning.update { true }
		}

		override fun onScanStop() {
			Logger.i(TAG) { "SCAN STOPPED!!" }
			_isScanning.update { false }
		}

		override fun onScanUpdated(peripheral: Peripheral?) {
			val result = peripheral ?: return
			Logger.d(TAG) { "SOME PERIPHERAL UPDATED ADDRESS:${result.address}" }
			val updatedList = _peers.value.map { device ->
				// if address already present update the rssi of the device
				if (device.deviceAddress == result.address.address()) device.copy(rssi = result.rssi)
				// else return the normal device
				else device
			}
			_peers.update { updatedList }
		}

		override fun onScanFound(peripheral: Peripheral?) {
			val result = peripheral ?: return
			Logger.d(TAG) { "SOME PERIPHERAL FOUND ADDRESS:${result.address}" }
			val address = result.address.address() ?: return
			val name = result.identifier ?: return
			val deviceAddresses = _peers.value.map { it.deviceAddress }
			// if it's a new device
			if (address in deviceAddresses) return
			val service: Service = result.services()
				.find { it.uuid == BLEConstants.transportServiceId.toHexDashString() }
				?: return

			val appId = service.data()

			var newDevice = BLEPeerDevice(
				deviceAddress = address,
				deviceName = name,
				rssi = result.rssi,
				appId = Uuid.fromByteArray(appId)
			)
			// add it to devices
			_peers.update { devices -> (devices + newDevice).distinctBy { it.deviceAddress } }

			// connect it
			peripheral.connect()
			try {
				val deviceNameBytes = service.characteristics
					.find { Uuid.parseHexDash(it.uuid) == BLEConstants.deviceNameCharacteristic }
					?.let { peripheral.read(BluetoothUUID(service.uuid), BluetoothUUID(it.uuid)) }
					?.decodeToString()
				val deviceNonceBytes = service.characteristics
					.find { Uuid.parseHexDash(it.uuid) == BLEConstants.deviceNonceCharacteristic }
					?.let { peripheral.read(BluetoothUUID(service.uuid), BluetoothUUID(it.uuid)) }
					?.decodeToString()

				val updatedDevice = newDevice.copy(
					deviceName = deviceNameBytes,
					deviceNonce = deviceNonceBytes
				)

				val updatedList = _peers.value.map { device ->
					if (device.deviceAddress == updatedDevice.deviceAddress) updatedDevice
					else device
				}
				_peers.update { updatedList }

			} finally {
				// disconnect it
				peripheral.disconnect()
			}
		}
	}

	override suspend fun startScan(timeout: Duration) {
		if (!Adapter.isBluetoothEnabled() || !hasBLEFeature || _isScanning.value) return
		// if a scan is running stop the scan
		if (_btAdapter?.scanIsActive == true) {
			Logger.i(TAG) { "SCAN CANCELLED" }
			_btAdapter?.scanStop()
		}

		try {
			// starts the scan
			startScanCallBack()
			// the function suspends for breaktime
			withContext(Dispatchers.Main) {
				delay(timeout)
			}
		} catch (e: Exception) {
			if (e is CancellationException) {
				Logger.i(TAG) { "SCAN CANCELLED" }
				throw e
			}
			e.printStackTrace()
		} finally {
			// if exception is thrown or the try block executed this will be oke
			stopScanning()
		}
	}

	private fun startScanCallBack() {
		if (_isScanning.value) return
		// updates is scanning
		_btAdapter?.scanStart()
		Logger.i(TAG) { "SCAN STARTED" }
	}

	override suspend fun stopScanning() {
		// scan is not running so nothing to stop
		if (!_isScanning.value) return
		// stop the scan details
		_isScanning.update { false }
		_btAdapter?.scanStop()
		Logger.i(TAG) { "SCAN STOPPED" }
	}

}