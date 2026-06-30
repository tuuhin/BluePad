package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattConnectionSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.callbacks.SyncDeviceConnectionCallback
import com.sam.bluepad.data.ble.callbacks.SyncDeviceDiscoveryCallback
import com.sam.bluepad.data.ble.exceptions.GattInvalidStatusException
import com.sam.bluepad.data.utils.hasBLEScanPermission
import com.sam.bluepad.data.utils.hasCoarseLocationPermission
import com.sam.bluepad.data.utils.hasFineLocationPermission
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.ResourcesSyncDataEvents
import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.exceptions.LocationDisabledException
import com.sam.bluepad.domain.exceptions.LocationPermissionException
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.uuid.toJavaUuid

private const val TAG = "SYNC_CONNECTION_MANAGER"

@SuppressLint("MissingPermission")
actual class BLESyncConnectionManagerImpl(
    private val context: Context,
    private val scanCallback: SyncDeviceDiscoveryCallback,
    private val connectionCallback: SyncDeviceConnectionCallback,
) : BLESyncConnectionManager {

    private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }
    private val _locationManager by lazy { context.getSystemService<LocationManager>() }

    private val _btAdapter: BluetoothAdapter?
        get() = _bluetoothManager?.adapter

    private val _btLEScanner: BluetoothLeScanner?
        get() = _bluetoothManager?.adapter?.bluetoothLeScanner

    private val _isLocationActive: Boolean
        get() = _locationManager?.isLocationEnabled ?: false

    private val _hasCorrectLocationPermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context.hasFineLocationPermission ||
            context.hasCoarseLocationPermission

    override fun discoverAndConnect(timeout: Duration): Flow<ResourcesSyncDataEvents> = flow {
        emit(Resource.Loading)

        val exc = checkPermissionsAndPreconditions()
        if (exc != null) {
            emit(Resource.Error(exc))
            return@flow
        }

        // if normal scan is running then stop it
        if (_btAdapter?.isDiscovering == true) _btAdapter?.cancelDiscovery()

        try {
            emit(Resource.Success(ConnectorSyncEvent.DiscoveryStarted))
            Logger.i(tag = TAG) { "SCANNING FOR DEVICES STATED" }
            val btDevice = withTimeout(timeout) {
                runBLEDiscovery().first()
            }
            Logger.i(tag = TAG) { "SCAN RESULT FOUND" }
            val identifier = btDevice.address.toString()
            emit(Resource.Success(ConnectorSyncEvent.DeviceFound(identifier)))
            // deals with the connection
            val connectionFlow = handleConnection(btDevice)
            emitAll(connectionFlow.map { Resource.Success(it) })
        } catch (_: TimeoutCancellationException) {
            Logger.w(tag = TAG) { "SCAN TIMEOUT ADVERTISEMENT NOT FOUND FOR TIMEOUT: $timeout" }
            emit(Resource.Success(ConnectorSyncEvent.DeviceScanTimeout))
        } catch (e: Exception) {
            if (e is CancellationException) {
                Logger.d(tag = TAG) { "CONNECTION JOB CANCELLED" }
                throw e
            }
            Logger.e(tag = TAG, throwable = e) { "SOME EXCEPTION OCCURRED" }
        }
    }.flowOn(Dispatchers.IO)


    private fun runBLEDiscovery(): Flow<BluetoothDevice> = channelFlow {

        scanCallback.onDeviceFound { trySend(it) }
        scanCallback.onError { err -> close(err) }

        val scanFilters = listOf<ScanFilter>(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BLEConstants.SYNC_SERVICE_ID.toJavaUuid()))
                .build(),
        )

        val scanSettings = ScanSettings.Builder()
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setLegacy(false)
            .build()

        Logger.i(tag = TAG) { "BLE SCAN STARTED" }
        _btLEScanner?.startScan(scanFilters, scanSettings, scanCallback) ?: close()

        awaitClose {
            Logger.i(tag = TAG) { "BLE SCAN STOPPED" }
            _btLEScanner?.stopScan(scanCallback)
            scanCallback.clearCallbacks()
        }
    }

    private fun handleConnection(device: BluetoothDevice) = channelFlow<ConnectorSyncEvent> {

        // callbacks
        connectionCallback.onEvents { event -> trySend(event) }
        connectionCallback.onError { err ->
            if (err is GattInvalidStatusException) {
                Logger.w(tag = TAG) { "BLE GATT STATUS EXCEPTION ERROR TEXT:${err.statusMessage}" }
            }
            close(err)
        }

        Logger.d(tag = TAG) { "OPENING GATT CONNECTION" }
        val btGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            val ioDispatcher = Dispatchers.IO
                .limitedParallelism(2, "bluetooth_connection_executor")
                .asExecutor()
            device.connectGatt(connectSettings, ioDispatcher, connectionCallback)
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(context, false, connectionCallback, BluetoothDevice.TRANSPORT_LE)
        }

        if (btGatt == null) close(IllegalStateException("Bluetooth GATT found to be null"))

        awaitClose {
            Logger.d(tag = TAG) { "CLOSING GATT CONNECTION" }
            btGatt?.close()
            connectionCallback.onClearCallbacks()
        }
    }

    override fun close() {
        Log.d(TAG, "CALLBACK CLEAN UP")
        connectionCallback.onClose()
    }

    private fun checkPermissionsAndPreconditions(): Exception? {
        return when {
            !context.hasBLEScanPermission -> BluetoothPermissionException()
            !_hasCorrectLocationPermission -> LocationPermissionException()
            _btAdapter?.isEnabled != true -> BluetoothNotEnabledException()
            !_isLocationActive -> LocationDisabledException()
            else -> null
        }
    }

    private val connectSettings: BluetoothGattConnectionSettings
        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        get() = BluetoothGattConnectionSettings.Builder()
            .setTransport(BluetoothDevice.TRANSPORT_LE)
            .setAutoConnectEnabled(false)
            .setOpportunisticEnabled(false)
            .build()
}
