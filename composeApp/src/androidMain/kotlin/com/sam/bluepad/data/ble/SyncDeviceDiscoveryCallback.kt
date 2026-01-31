package com.sam.bluepad.data.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import co.touchlab.kermit.Logger
import com.sam.bluepad.BuildKonfig
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.exceptions.BLEDiscoveryException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.uuid.toJavaUuid

private const val TAG = "SYNC_DISCOVERY_CALLBACK"

class SyncDeviceDiscoveryCallback : ScanCallback() {

    private val _devicesChannel = Channel<BluetoothDevice>(Channel.CONFLATED)
    private val _errorsChannel = Channel<Exception>(capacity = Channel.CONFLATED)

    val devicesFlow = _devicesChannel.receiveAsFlow()
    val errorsFlow = _errorsChannel.receiveAsFlow()

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        val scanRecord = result?.scanRecord ?: return
        val device = result.device ?: return

        Logger.d(TAG) { "SCAN RESULTS FOUND" }
        val parcelUid = ParcelUuid(BLEConstants.SYNC_SERVICE_ID.toJavaUuid())
        val hasServiceId = (scanRecord.serviceUuids ?: emptyList()).contains(parcelUid)

        if (!hasServiceId) return

        val serviceData = scanRecord.getServiceData(parcelUid)
        val readableData = serviceData?.joinToString("-") { it.toHexString() }
        Logger.d(TAG) { "SCAN RESULT FOUND UUID:${parcelUid.uuid} DATA:$readableData" }
        if (serviceData?.decodeToString() != BuildKonfig.APP_ID) return
        // send the device
        _devicesChannel.trySend(device)
    }

    override fun onScanFailed(errorCode: Int) {
        val exception = when (errorCode) {
            SCAN_FAILED_ALREADY_STARTED -> BLEDiscoveryException("Scan already started")
            SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> BLEDiscoveryException("Application failed to registered")
            SCAN_FAILED_INTERNAL_ERROR -> BLEDiscoveryException("Internal error")
            SCAN_FAILED_FEATURE_UNSUPPORTED -> BLEDiscoveryException("Scan configuration is unsupported")
            SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> BLEDiscoveryException("Bluetooth LE is not available")
            SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> BLEDiscoveryException("Scanning too frequently")
            else -> return
        }
        _errorsChannel.trySend(exception)
        Logger.e(TAG, exception) { "FAILED TO SEND ERROR CODE : $errorCode" }
    }
}