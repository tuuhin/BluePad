package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.ble_common.BluetoothInfoProvider
import com.sam.blejavaadvertise.BLEAdvertiser
import com.sam.blejavaadvertise.models.GattAdvertisementConfig
import com.sam.bluepad.BuildKonfig
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionType
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.exceptions.BLEAdvertiseUnsupportedException
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val TAG = "BLE_ADVERTISER"

actual class BLEAdvertisementImpl(
	private val callback: BLEAdvertisementCallback
) : BLEAdvertisementManager {

	private val _advertiser by lazy { BLEAdvertiser() }

	override val isRunning: Flow<Boolean>
		get() = callback.isRunning

	override val errorFlow: Flow<Exception>
		get() = emptyFlow()

	override val peerSaveDevices: Flow<List<BLEPeerData>>
		get() = callback.externalPeers

	override suspend fun startAdvertising(type: BLEConnectionType): Result<Unit> {

		if (!BluetoothInfoProvider.isBluetoothActive())
			return Result.failure(BluetoothNotEnabledException())

		if (!BluetoothInfoProvider.isLEConnectionAllowed())
			return Result.failure(BLENotSupportedException())

		if (!BluetoothInfoProvider.isPeripheralRoleSupported())
			return Result.failure(BLEAdvertiseUnsupportedException())

		_advertiser.setListener(callback)
		return try {
			_advertiser.startServer()
			when (type) {
				BLEConnectionType.DEVICE_DISCOVERY -> {
					_advertiser.addService(BLEServiceToGatt.deviceDiscoveryService)
					Logger.d(TAG) { "BLE ADVERTISEMENT FOR DEVICE DISCOVERY" }
				}

				BLEConnectionType.PROXIMITY_AND_SYNC -> {
					_advertiser.addService(BLEServiceToGatt.deviceSyncService)
					Logger.d(TAG) { "BLE ADVERTISEMENT FOR SYNC " }
				}
			}

			val data = BuildKonfig.APP_ID.encodeToByteArray()
			val config = GattAdvertisementConfig(true, true, data)
			_advertiser.startAdvertisement(config)
			Result.success(Unit)
		} catch (e: Exception) {
			Result.failure(e)
		}
	}

	override fun stopAdvertising() {
		_advertiser.stopServer()
		callback.setRunning(false)
		Logger.i(TAG) { "ADVERTISEMENT STOPPED" }
	}

	override fun cleanUp() {
		callback.cleanUp()
		Logger.i(TAG) { "STOPPING GATT SERVER AND CLEANING UP" }
		_advertiser.stopServer()
	}
}