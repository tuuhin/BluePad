package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.hasAdvertisePermission
import com.sam.bluepad.data.utils.hasConnectPermission
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlin.uuid.toJavaUuid

private const val TAG = "BLEAdvertisementImpl"

@SuppressLint("MissingPermission")
actual class BLEAdvertisementImpl(private val context: Context) : BLEAdvertisementManager {

	private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }
	private val _connectionCallback by lazy { ServerConnectionCallback(context) }
	private val _advertiseCallback by lazy { ServerAdvertisementCallback() }

	private val _isRunning = MutableStateFlow(false)

	override val isRunning: Flow<Boolean>
		get() = merge(_isRunning, _advertiseCallback.isRunning)


	private var _bleServer: BluetoothGattServer? = null

	override fun startAdvertising(deviceName: String, nonce: String?) {
		if (!checkCanRunServer()) return

		val advertiser = _bluetoothManager?.adapter?.bluetoothLeAdvertiser ?: return

		_bleServer = _bluetoothManager?.openGattServer(context, _connectionCallback)
		Logger.i(TAG) { "GATT SERVER BEGUN!" }

		_connectionCallback.setOnServiceAdded { Logger.d(TAG) { "TRANSPORT SERVICE ADDED " } }
		_connectionCallback.setOnSendResponse { device, requestId, status, offset, value ->
			_bleServer?.sendResponse(device, requestId, status, offset, value)
			Logger.d(TAG) { "READ RESPONSE SEND :$status" }
		}
		_bleServer?.addService(transportService)
		// advertisement settings
		val settingsBuilder = AdvertiseSettings.Builder()
			.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
			.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
			.setConnectable(true)
			.setTimeout(0)

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
			settingsBuilder.setDiscoverable(true)

		val advertiseSettings = settingsBuilder.build()

		val transportServiceId = BLEConstants.transportServiceId.toJavaUuid()
		val bytes = BLEConstants.transportServiceData.toByteArray()
		// what to advertise
		val advertiseData = AdvertiseData.Builder()
			.setIncludeDeviceName(true)
			.setIncludeTxPowerLevel(true)
			.addServiceData(ParcelUuid(transportServiceId), bytes)
			.build()

		Logger.d(TAG) { "STARTING ADVERTISEMENT" }
		// start advertising
		advertiser.startAdvertising(advertiseSettings, advertiseData, _advertiseCallback)
	}

	override fun stopAdvertising() {
		Logger.i(TAG) { "STOPPING ADVERTISEMENT" }
		val advertiser = _bluetoothManager?.adapter?.bluetoothLeAdvertiser ?: return
		advertiser.stopAdvertising(_advertiseCallback)
		_isRunning.update { false }

		_bleServer?.clearServices()
		Logger.d(TAG) { "STOPPING SERVER" }
		_bleServer?.close()
		_bleServer = null
	}

	override fun cleanUp() {
		_connectionCallback.cleanUp()
		stopAdvertising()
	}

	private fun checkCanRunServer(): Boolean {
		return context.hasConnectPermission && context.hasAdvertisePermission &&
				_bluetoothManager?.adapter?.isEnabled == true &&
				_bluetoothManager?.adapter?.isMultipleAdvertisementSupported == false
	}

	companion object {
		private val transportService = BluetoothGattService(
			BLEConstants.transportServiceId.toJavaUuid(),
			BluetoothGattService.SERVICE_TYPE_PRIMARY
		).apply {
			addCharacteristic(
				BluetoothGattCharacteristic(
					BLEConstants.deviceNameCharacteristic.toJavaUuid(),
					BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
				)
			)
			addCharacteristic(
				BluetoothGattCharacteristic(
					BLEConstants.deviceNonceCharacteristic.toJavaUuid(),
					BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
				)
			)
		}
	}
}