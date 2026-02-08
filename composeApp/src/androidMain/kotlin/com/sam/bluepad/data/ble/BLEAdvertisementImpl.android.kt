package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
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

private const val TAG = "BLE_ADVERTISEMENT"

@SuppressLint("MissingPermission")
actual class BLEAdvertisementImpl(
    private val context: Context,
    private val advertizerConfig: BLEGattAdvertiserConfig,
    private val connectionCallback: ServerConnectionCallback,
) : BLEAdvertisementManager {

    private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

    private val _btAdapter: BluetoothAdapter?
        get() = _bluetoothManager?.adapter

    override val isRunning: Flow<Boolean>
        get() = advertizerConfig.isAdvertisementRunning

    override val errorFlow: Flow<Exception>
        get() = advertizerConfig.errorsFlow

    override val peerSaveDevices: Flow<List<BLEPeerData>>
        get() = connectionCallback.incomingPeerData

    override val serverSyncEvents: Flow<BLEServerSyncEvent>
        get() = connectionCallback.syncRequestEvents

    private var _bleServer: BluetoothGattServer? = null

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

        _bleServer = _bluetoothManager?.openGattServer(context, connectionCallback)
        Logger.i(TAG) { "GATT SERVER BEGUN!" }

        connectionCallback.setOnServiceAdded { Logger.d(TAG) { "SERVICE ADDED " } }
        connectionCallback.setOnSendResponse { device, requestId, status, offset, value ->
            val isSuccess = _bleServer?.sendResponse(device, requestId, status, offset, value)
            Logger.d(TAG) { "SERVER RESPONSE SEND SUCCESS: $isSuccess" }
        }
        connectionCallback.setNotifyCharacteristicsChanged(::onNotifyCharacteristics)

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
            BLEConnectionType.DEVICE_DISCOVERY -> BLEConstants.DEVICE_INFO_SERVICE_ID
            BLEConnectionType.PROXIMITY_AND_SYNC -> BLEConstants.SYNC_SERVICE_ID
        }

        advertizerConfig.startAdvertisingSet(advertisementServiceId, BuildKonfig.APP_ID.encodeToByteArray())
        return Result.success(Unit)
    }

    @Suppress("DEPRECATION")
    private fun onNotifyCharacteristics(
        device: BluetoothDevice,
        characteristics: BluetoothGattCharacteristic,
        confirm: Boolean,
        byteArray: ByteArray
    ): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // values are set independent
            val status =
                _bleServer?.notifyCharacteristicChanged(device, characteristics, confirm, byteArray)
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


    override fun stopAdvertising() {
        try {
            advertizerConfig.removeAdvertisingSet()
            _bleServer?.clearServices()
            Logger.d(TAG) { "STOPPING SERVER" }
            _bleServer?.close()
        } catch (e: Exception) {
            Logger.d(TAG, e) { "SOME EXCEPTIONS" }
        } finally {
            _bleServer = null
        }
    }

    override fun cleanUp() {
        connectionCallback.cleanUp()
        stopAdvertising()
    }
}