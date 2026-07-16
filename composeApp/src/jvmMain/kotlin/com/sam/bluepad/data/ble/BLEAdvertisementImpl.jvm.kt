package com.sam.bluepad.data.ble

import co.touchlab.kermit.Logger
import com.sam.ble_advertise.extension.addService
import com.sam.ble_advertise.extension.setListener
import com.sam.ble_advertise.platform.GATTAdvertiseConfig
import com.sam.ble_advertise.platform.PlatformBLEAdvertiser
import com.sam.bluepad.BuildKonfig
import com.sam.bluepad.data.ble.callbacks.BLEAdvertisementCallback
import com.sam.bluepad.data.ble.utils.BLEServiceToGatt
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionType
import com.sam.bluepad.domain.ble.events.AdvertiserSyncEvent
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.exceptions.BLEAdvertiseUnsupportedException
import com.sam.bluepad.domain.exceptions.BLENotSupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bt_common.isBTActive
import com.sam.bt_common.isLEConnectionAvailable
import com.sam.bt_common.isPeripheralRoleSupported
import com.sam.bt_common.platform.PlatformBTInfoProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

private const val TAG = "BLE_ADVERTISER"

actual class BLEAdvertisementImpl(
    private val callback: BLEAdvertisementCallback,
    private val platformDispatchers: PlatformDispatcherProvider,
) : BLEAdvertisementManager {

    private val _advertiser by lazy { PlatformBLEAdvertiser() }

    private val _scope = CoroutineScope(platformDispatchers.io + SupervisorJob())

    override val isRunning: Flow<Boolean>
        get() = callback.isRunning

    override val errorFlow: Flow<Exception>
        get() = emptyFlow()

    override val peerSaveDevices: Flow<List<BLEPeerData>>
        get() = callback.incomingDeviceData

    override val serverSyncEvents: Flow<AdvertiserSyncEvent>
        get() = callback.syncRequestEvents

    override suspend fun startAdvertising(type: BLEConnectionType): Result<Unit> {

        if (!PlatformBTInfoProvider.isBTActive())
            return Result.failure(BluetoothNotEnabledException())

        if (!PlatformBTInfoProvider.isLEConnectionAvailable())
            return Result.failure(BLENotSupportedException())

        if (!PlatformBTInfoProvider.isPeripheralRoleSupported())
            return Result.failure(BLEAdvertiseUnsupportedException())

        return withContext(Dispatchers.IO) {
            try {
                _advertiser.setListener(callback, _scope)
                callback.setNotifyCharacteristicsChanged(::handleNotification)

                when (type) {
                    BLEConnectionType.DEVICE_DISCOVERY -> {
                        _advertiser.addService(BLEServiceToGatt.deviceDiscoveryService)
                        Logger.d(tag = TAG) { "BLE ADVERTISEMENT FOR DEVICE DISCOVERY" }
                    }

                    BLEConnectionType.PROXIMITY_AND_SYNC -> {
                        _advertiser.addService(BLEServiceToGatt.deviceSyncService)
                        Logger.d(tag = TAG) { "BLE ADVERTISEMENT FOR SYNC " }
                    }
                }

                val data: String = BuildKonfig.APP_ID
                val config = GATTAdvertiseConfig(discoverable = true, connectable = true, serviceData = data)
                _advertiser.start(config)
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    private suspend fun handleNotification(address: String, uuid: Uuid, value: ByteArray): Boolean {
        return withContext(platformDispatchers.io) {
            try {
                Logger.d(tag = TAG) { "SENDING NOTIFICATION" }
                val result = _advertiser.sendNotification(
                    deviceAddress = address,
                    characteristicUuid = uuid.toString(),
                    value = value,
                )
                Logger.d(tag = TAG) { "NOTIFICATION SEND :$result" }
                result
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "FAILED TO SEND NOTIFICATION" }
                false
            }
        }
    }

    override fun stopAdvertising() {
        _advertiser.stop()
        callback.setRunning(false)
        Logger.i(tag = TAG) { "ADVERTISEMENT STOPPED" }
    }

    override fun cleanUp() {
        if (_scope.isActive) {
            Logger.i(tag = TAG) { "CLEARING UP THE SCOPE" }
            _scope.cancel()
        }
        callback.cleanUp()
        Logger.i(tag = TAG) { "STOPPING GATT SERVER AND CLEANING UP RESOURCES" }
        val isActive = runBlocking {
            PlatformBTInfoProvider.isBTActive()
        }
        if (!isActive) return
        Logger.d(tag = TAG) { "DESTROYING ADVERTISER REF" }
        _advertiser.onDestroy()
        _advertiser.close()
    }
}
