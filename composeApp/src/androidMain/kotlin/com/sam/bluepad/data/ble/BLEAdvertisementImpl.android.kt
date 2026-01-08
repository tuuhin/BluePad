package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.os.ParcelUuid
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.BuildKonfig
import com.sam.bluepad.data.utils.hasAdvertisePermission
import com.sam.bluepad.data.utils.hasConnectPermission
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.exceptions.BLEAdvertisePermissionException
import com.sam.bluepad.domain.exceptions.BLEAdvertiseUnsupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.uuid.toJavaUuid

private const val TAG = "BLE_ADVERTISEMENT"

@SuppressLint("MissingPermission")
actual class BLEAdvertisementImpl(
	private val context: Context,
	private val infoProvider: LocalDeviceInfoProvider,
	private val randomGenerator: RandomGenerator,
) : BLEAdvertisementManager {

	private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }
	private val _connectionCallback by lazy {
		ServerConnectionCallback(infoProvider, randomGenerator)
	}

	private val _isRunning = MutableStateFlow(false)
	private val _errorsChannel = Channel<Exception>(capacity = Channel.CONFLATED)

	private val _advertiseCallback = object : AdvertisingSetCallback() {

		override fun onAdvertisingSetStarted(
			advertisingSet: AdvertisingSet?, txPower: Int, status: Int
		) {
			if (status == ADVERTISE_SUCCESS) {
				Logger.d(TAG) { "BLE5 ADVERTISING STARTED txPower=$txPower" }
				_isRunning.value = true
			} else {
				onStartFailure(status)
			}
		}

		override fun onAdvertisingEnabled(
			dvertisingSet: AdvertisingSet?, enable: Boolean, status: Int
		) {
			if (status != ADVERTISE_SUCCESS && !enable) return
			Logger.d(TAG) { "BLE5 ADVERTISING ENABLED" }
		}

		override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
			Logger.d(TAG) { "BLE5 ADVERTISING STOPPED" }
			_isRunning.value = false
		}
	}

	override val isRunning: Flow<Boolean>
		get() = _isRunning.asStateFlow()

	override val errorFlow
		get() = _errorsChannel.receiveAsFlow()

	private var _bleServer: BluetoothGattServer? = null

	override suspend fun startAdvertising(): Result<Unit> {

		val isExtendedSupported = _bluetoothManager?.adapter
			?.isLeExtendedAdvertisingSupported ?: false
		val isMultipleSupported = _bluetoothManager?.adapter
			?.isMultipleAdvertisementSupported ?: false

		if (!context.hasConnectPermission)
			return Result.failure(BluetoothPermissionException())
		if (!context.hasAdvertisePermission)
			return Result.failure(BLEAdvertisePermissionException())
		if (_bluetoothManager?.adapter?.isEnabled != true)
			return Result.failure(BluetoothNotEnabledException())
		if (!isExtendedSupported || !isMultipleSupported)
			return Result.failure(BLEAdvertiseUnsupportedException())


		val advertiser = _bluetoothManager?.adapter
			?.bluetoothLeAdvertiser ?: return Result.failure(BLEAdvertiseUnsupportedException())

		_bleServer = _bluetoothManager?.openGattServer(context, _connectionCallback)
		Logger.i(TAG) { "GATT SERVER BEGUN!" }

		_connectionCallback.setOnServiceAdded { Logger.d(TAG) { "TRANSPORT SERVICE ADDED " } }
		_connectionCallback.setOnSendResponse { device, requestId, status, offset, value ->
			_bleServer?.sendResponse(device, requestId, status, offset, value)
			Logger.d(TAG) { "READ RESPONSE SEND :$status" }
		}
		_bleServer?.addService(transportService)
		// advertisement settings
		val parameters = AdvertisingSetParameters.Builder()
			.setLegacyMode(false)
			.setConnectable(false)
			.setScannable(true)
			.setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
			.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
			.setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
			.setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
			.build()

		// what to advertise
		val advertiseData = AdvertiseData.Builder()
			.setIncludeDeviceName(false)
			.setIncludeTxPowerLevel(false)
			.addServiceUuid(ParcelUuid(BLEConstants.transportServiceId.toJavaUuid()))

			.build()

		val scanResponse = AdvertiseData.Builder()
			.setIncludeTxPowerLevel(false)
			.setIncludeDeviceName(true)
			.addServiceUuid(ParcelUuid(BLEConstants.transportServiceId.toJavaUuid()))
			.addServiceData(
				ParcelUuid(BLEConstants.transportServiceId.toJavaUuid()),
				BuildKonfig.APP_ID.encodeToByteArray()
			)
			.build()

		Logger.d(TAG) { "STARTING ADVERTISEMENT" }
		// start advertising
		advertiser.startAdvertisingSet(
			parameters, advertiseData, scanResponse,
			null, null, _advertiseCallback
		)
		return Result.success(Unit)
	}

	override fun stopAdvertising() {
		val advertiser = _bluetoothManager?.adapter?.bluetoothLeAdvertiser ?: return
		Logger.i(TAG) { "STOPPING ADVERTISEMENT" }
		advertiser.stopAdvertisingSet(_advertiseCallback)
		_isRunning.value = false

		_bleServer?.clearServices()
		Logger.d(TAG) { "STOPPING SERVER" }
		_bleServer?.close()
		_bleServer = null
	}

	override fun cleanUp() {
		_connectionCallback.cleanUp()
		stopAdvertising()
	}

	private fun onStartFailure(errorCode: Int) {
		val exception = when (errorCode) {
			AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED -> Exception("Advertisement is already running")
			AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Exception("Too many advertiser")
			AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> Exception("Android cannot start advertisement")
			AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> Exception("BLE not supported")
			else -> Exception("Cannot start the advertisement ERROR CODE: $errorCode")
		}
		Logger.e(TAG, exception) { "GATT SERVER FAILED TO START ERROR CODE: $errorCode" }
		_errorsChannel.trySend(exception)
	}

	companion object {
		private val transportService = BluetoothGattService(
			BLEConstants.transportServiceId.toJavaUuid(),
			BluetoothGattService.SERVICE_TYPE_PRIMARY
		).apply {
			addCharacteristic(
				BluetoothGattCharacteristic(
					BLEConstants.deviceIdCharacteristics.toJavaUuid(),
					BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ
				)
			)
			addCharacteristic(
				BluetoothGattCharacteristic(
					BLEConstants.deviceNameCharacteristic.toJavaUuid(),
					BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ
				)
			)
			addCharacteristic(
				BluetoothGattCharacteristic(
					BLEConstants.deviceNonceCharacteristic.toJavaUuid(),
					BluetoothGattCharacteristic.PROPERTY_READ,
					BluetoothGattCharacteristic.PERMISSION_READ
				)
			)
		}
	}
}