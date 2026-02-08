package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSetParameters
import android.content.Context
import android.os.ParcelUuid
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val TAG = "BLE_GATT_ADVERTISEMENT_CONFIG"

class BLEGattAdvertiserConfig(
    private val context: Context,
    private val callback: BLEGattAdvertisementCallback,
) {
    val isAdvertisementRunning: StateFlow<Boolean> = callback.isRunning
    val errorsFlow: Flow<Exception> = callback.errorsFlow

    private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

    fun startAdvertisingSet(serviceId: Uuid, serviceData: ByteArray) {

        val advertiser = _bluetoothManager?.adapter?.bluetoothLeAdvertiser ?: return
        val advertisementServiceId = ParcelUuid(serviceId.toJavaUuid())

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
            .addServiceData(advertisementServiceId, serviceData)
            .build()

        val scanResponse = AdvertiseData.Builder()
            .setIncludeTxPowerLevel(false)
            .setIncludeDeviceName(true)
            .addServiceUuid(advertisementServiceId)
            .addServiceData(advertisementServiceId, serviceData)
            .build()

        Logger.d(TAG) { "STARTING ADVERTISEMENT" }
        // start advertising
        advertiser.startAdvertisingSet(
            parameters, advertiseData, scanResponse,
            null, null, callback
        )
    }

    @SuppressLint("MissingPermission")
    fun removeAdvertisingSet() {
        try {
            val advertiser = _bluetoothManager?.adapter?.bluetoothLeAdvertiser ?: return
            Logger.i(TAG) { "STOPPING ADVERTISEMENT" }
            advertiser.stopAdvertisingSet(callback)
        } finally {
            callback.setRunning(false)
        }
    }

}