package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.BuildKonfig
import com.sam.bluepad.data.utils.hasAdvertisePermission
import com.sam.bluepad.data.utils.hasConnectPermission
import com.sam.bluepad.domain.ble.BLEAdvertisementManager
import com.sam.bluepad.domain.ble.BLEConnectionType
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.ble.models.BLEServerSyncEvent
import com.sam.bluepad.domain.exceptions.BLEAdvertisePermissionException
import com.sam.bluepad.domain.exceptions.BLEAdvertiseUnsupportedException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.toJavaUuid

private const val TAG = "BLE_ADVERTISEMENT"

@SuppressLint("MissingPermission")
actual class BLEAdvertisementImpl(
    private val context: Context,
    private val advertisementCallback: BLEGattAdvertisementCallback,
    private val connectionCallback: ServerConnectionCallback,
) : BLEAdvertisementManager {

    private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

    private val _btAdapter: BluetoothAdapter?
        get() = _bluetoothManager?.adapter

    override val isRunning: Flow<Boolean>
        get() = advertisementCallback.isRunning

    override val errorFlow: Flow<Exception>
        get() = advertisementCallback.errorsFlow

    override val peerSaveDevices: Flow<List<BLEPeerData>>
        get() = connectionCallback.incomingPeerData

    override val serverSyncEvents: Flow<BLEServerSyncEvent>
        get() = connectionCallback.syncRequestEvents

    private var _bleServer: BluetoothGattServer? = null

    @Suppress("DEPRECATION")
    override suspend fun startAdvertising(type: BLEConnectionType): Result<Unit> {

        val isExtendedSupported = _btAdapter?.isLeExtendedAdvertisingSupported ?: false
        val isMultipleSupported = _btAdapter?.isMultipleAdvertisementSupported ?: false

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

        _bleServer = _bluetoothManager?.openGattServer(context, connectionCallback)
        Logger.i(TAG) { "GATT SERVER BEGUN!" }

        connectionCallback.setOnServiceAdded { Logger.d(TAG) { "SERVICE ADDED " } }
        connectionCallback.setOnSendResponse { device, requestId, status, offset, value ->
            val isSuccess = _bleServer?.sendResponse(device, requestId, status, offset, value)
            Logger.d(TAG) { "SERVER RESPONSE SEND SUCCESS: $isSuccess" }
        }

        connectionCallback.setNotifyCharacteristicsChanged { device, characteristics, confirm, byteArray ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // values are set independent
                    val status = _bleServer
                        ?.notifyCharacteristicChanged(device, characteristics, confirm, byteArray)
                    status == BluetoothStatusCodes.SUCCESS
                } else {
                    characteristics.value = byteArray
                    _bleServer?.notifyCharacteristicChanged(device, characteristics, confirm)
                        ?: false
                }
            } catch (e: IllegalStateException) {
                Logger.w(TAG, e) { "ILLEGAL ARGUMENT CHARACTERISTIC VALUE CANNOT BE BLANK " }
                false
            }
        }

        when (type) {
            BLEConnectionType.DEVICE_DISCOVERY -> {
                _bleServer?.addService(BLEServiceToGatt.deviceDiscoveryService)
                Logger.d(TAG) { "BLE ADVERTISEMENT FOR DEVICE DISCOVERY" }
            }

            BLEConnectionType.PROXIMITY_AND_SYNC -> {
                _bleServer?.addService(BLEServiceToGatt.deviceSyncService)
                Logger.d(TAG) { "BLE ADVERTISEMENT FOR SYNC " }
            }
        }

        val advertisementServiceId = when (type) {
            BLEConnectionType.DEVICE_DISCOVERY -> ParcelUuid(BLEConstants.DEVICE_INFO_SERVICE_ID.toJavaUuid())
            BLEConnectionType.PROXIMITY_AND_SYNC -> ParcelUuid(BLEConstants.SYNC_SERVICE_ID.toJavaUuid())
        }

        // advertisement settings
        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(false)
            .setConnectable(true)
            .setScannable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
            .build()

        // what to advertise
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(advertisementServiceId)
            .addServiceData(advertisementServiceId, BuildKonfig.APP_ID.encodeToByteArray())
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeTxPowerLevel(false)
            .setIncludeDeviceName(true)
            .addServiceUuid(advertisementServiceId)
            .addServiceData(advertisementServiceId, BuildKonfig.APP_ID.encodeToByteArray())
            .build()

        Logger.d(TAG) { "STARTING ADVERTISEMENT" }
        // start advertising
        advertiser.startAdvertisingSet(
            parameters, advertiseData, scanResponse,
            null, null, advertisementCallback
        )
        return Result.success(Unit)
    }

    override fun stopAdvertising() {
        try {
            val advertiser = _bluetoothManager?.adapter?.bluetoothLeAdvertiser ?: return
            Logger.i(TAG) { "STOPPING ADVERTISEMENT" }
            advertiser.stopAdvertisingSet(advertisementCallback)

            _bleServer?.clearServices()
            Logger.d(TAG) { "STOPPING SERVER" }
            _bleServer?.close()
        } catch (e: Exception) {
            Logger.d(TAG, e) { "SOME EXCEPTIONS" }
        } finally {
            advertisementCallback.setRunning(false)
            _bleServer = null
        }
    }

    override fun cleanUp() {
        connectionCallback.cleanUp()
        stopAdvertising()
    }
}