package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.ble_common.Characteristic
import com.sam.ble_common.Service
import com.sam.blejavaadvertise.BLEAdvertiser
import com.sam.blejavaadvertise.callbacks.GATTServerCallback
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus
import com.sam.blejavaadvertise.models.GattAdvertisementConfig
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.simplejavable.Adapter

private const val TAG = "BLE_ADVERTISER"

actual class BLEAdvertisementImpl : BLEAdvertisementManager {

	private val _isRunning = MutableStateFlow(false)

	private val _advertiser by lazy { BLEAdvertiser() }

	override val isRunning: Flow<Boolean>
		get() = _isRunning

	private val _callback = object : GATTServerCallback {
		override fun onServiceAdded(serviceUuid: String, success: Boolean, error: Int) {
			Logger.i(TAG) { "Service $serviceUuid added = $success" }
		}

		override fun onServiceStatusChange(status: GATTServiceAdvertisementStatus?) {
			Logger.d(TAG) { "ADVERTISEMENT STATUS :$status" }
			_isRunning.value = status == GATTServiceAdvertisementStatus.STARTED
		}

		override fun onReadCharacteristics(serviceUuid: String?, characteristicUuid: String?)
				: ByteArray? {
			if (serviceUuid == null) return return null
			if (characteristicUuid == null) return null

			return when (characteristicUuid.lowercase()) {
				BLEConstants.deviceNameCharacteristic.toHexString() -> "Sam boult".encodeToByteArray()
				BLEConstants.deviceNonceCharacteristic.toHexString() -> "sdiufdbfiudf".encodeToByteArray()
				else -> byteArrayOf()
			}
		}
	}

	override fun startAdvertising(deviceName: String, nonce: String?) {
		if (!Adapter.isBluetoothEnabled()) {
			Logger.e(TAG) { "BLE IS NOT TURNED ON" }
			return
		}
		if (!_advertiser.hasLEPeripheralRoleSupport()) {
			Logger.e(TAG) { "DON'T HAVE A BLE PERIPHERAL SUPPORT" }
			return
		}

		_advertiser.setListener(_callback)
		_advertiser.startServer()
		_advertiser.addService(transportService)

		val data = BLEConstants.transportServiceData.toByteArray()

		val config = GattAdvertisementConfig(true, true, data)
		_advertiser.startAdvertisement(config)
	}

	override fun stopAdvertising() {
		_advertiser.stopAdvertisement()
		_isRunning.value = false
		Logger.i(TAG) { "ADVERTISEMENT STOPPED" }
	}

	override fun cleanUp() {
		Logger.i(TAG) { "GATT SERVER STOPPED" }
		_advertiser.stopServer()
	}

	companion object {
		private val transportService = Service(
			BLEConstants.transportServiceId.toHexDashString(),
			byteArrayOf(),
			listOf(
				Characteristic(
					BLEConstants.deviceNameCharacteristic.toHexDashString(),
					emptyList(),
					true,
					false,
					false,
					false, false
				),
				Characteristic(
					BLEConstants.deviceNonceCharacteristic.toHexDashString(),
					emptyList(),
					true,
					false,
					false,
					false, false
				),
			)
		)
	}
}