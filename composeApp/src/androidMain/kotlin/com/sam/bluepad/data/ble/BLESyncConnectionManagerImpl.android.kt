package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.hasBLEScanPermission
import com.sam.bluepad.data.utils.hasCoarseLocationPermission
import com.sam.bluepad.data.utils.hasFineLocationPermission
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.BLESyncConnectionManager
import com.sam.bluepad.domain.ble.ResourcesSyncDataEvents
import com.sam.bluepad.domain.ble.models.BLEDeviceSyncEvent
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.exceptions.LocationDisabledException
import com.sam.bluepad.domain.exceptions.LocationPermissionException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.time.Duration
import kotlin.uuid.toJavaUuid

private const val TAG = "SYNC_CONNECTION_MANAGER"

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
actual class BLESyncConnectionManagerImpl(
    private val context: Context,
    private val protoBuf: ProtoBuf,
    private val deviceInfoProvider: LocalDeviceInfoProvider,
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
            emit(Resource.Success(BLEDeviceSyncEvent.DiscoveryStarted))
            Logger.i(TAG) { "SCANNING FOR DEVICES STATED" }
            val btDevice = withTimeout(timeout) {
                runBLEDiscovery().first()
            }
            Logger.i(TAG) { "SCAN RESULT FOUND" }
            val identifier = btDevice.address.toString()
            emit(Resource.Success(BLEDeviceSyncEvent.DeviceFound(identifier)))
            // deals with the connection
            val connectionFlow = handleConnection(btDevice)
            emitAll(connectionFlow)
        } catch (_: TimeoutCancellationException) {
            Logger.w(TAG) { "SCAN TIMEOUT ADVERTISEMENT NOT FOUND FOR TIMEOUT: $timeout" }
            emit(Resource.Success(BLEDeviceSyncEvent.DeviceScanTimeout))
        } catch (e: Exception) {
            if (e is CancellationException) {
                Logger.d(TAG) { "CONNECTION JOB CANCELLED" }
                throw e
            }
            Logger.e(TAG, e) { "SOME EXCEPTION OCCURRED" }
        }
    }.flowOn(Dispatchers.IO)


    private fun runBLEDiscovery(): Flow<BluetoothDevice> = channelFlow {

        val scanCallback = SyncDeviceDiscoveryCallback()

        launch(Dispatchers.Main) {
            // in case any device is found collect the device
            scanCallback.devicesFlow.collect(::send)
        }

        launch(Dispatchers.Main) {
            // if any errors happens throw the error and close the channel
            val error = scanCallback.errorsFlow.first()
            close()
            throw error
        }

        val scanFilters = listOf<ScanFilter>(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BLEConstants.SYNC_SERVICE_ID.toJavaUuid()))
                .build()
        )

        val scanSettings = ScanSettings.Builder()
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setLegacy(false)
            .build()

        Log.i(TAG, "BLE SCAN STARTED")
        _btLEScanner?.startScan(scanFilters, scanSettings, scanCallback) ?: close()

        awaitClose {
            Log.i(TAG, "BLE SCAN STOPPED")
            _btLEScanner?.stopScan(scanCallback)
        }
    }

    private fun handleConnection(device: BluetoothDevice) = channelFlow<ResourcesSyncDataEvents> {

        val gattCallback = SyncDeviceConnectionCallback(
            protoBuf = protoBuf,
            provider = deviceInfoProvider
        )

        // handle the errors
        launch {
            gattCallback.errorFlow.collect { err ->
                send(Resource.Error(err))
            }
        }

        // handle the events
        launch {
            gattCallback.eventsFlow.collect { event ->
                send(Resource.Success(event))
            }
        }

        Log.d(TAG, "OPENING GATT CONNECTION")
        val gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

        awaitClose {
            Log.d(TAG, "CLOSING GATT CONNECTION")
            gattCallback.cancel()
            gatt.close()
        }
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

    override fun cleanUp() = Unit
}