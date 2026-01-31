package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

private const val TAG = "BLE_CONNECTION_CALLBACK"

@SuppressLint("MissingPermission")
class DeviceConnectionCallback(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val onConnectionStateChange: (gatt: BluetoothGatt?, state: BLEConnectionState) -> Unit,
    private val onGAttFailed: (String) -> Unit,
    private val onReadCharacteristic: suspend (BluetoothGatt, Uuid, ByteArray) -> Unit,
    private val onWriteCharacteristic: suspend (BluetoothGatt, Uuid) -> Unit,
) : BluetoothGattCallback() {

    private val _readQueue = ConcurrentLinkedQueue<BluetoothGattCharacteristic>()

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        val domainState = when (newState) {
            BluetoothGatt.STATE_CONNECTED -> BLEConnectionState.CONNECTED
            BluetoothGatt.STATE_CONNECTING -> BLEConnectionState.CONNECTING
            BluetoothGatt.STATE_DISCONNECTING -> BLEConnectionState.DISCONNECTING
            BluetoothGatt.STATE_DISCONNECTED -> BLEConnectionState.DISCONNECTED
            else -> return
        }
        Logger.d(TAG) { "CONNECTION STATE CHANGED! :$domainState" }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            onGAttFailed("Cannot read connection state")
            Logger.w(TAG) { "CONNECTION STATUS FAILED :$domainState" }
            // clear the queue if anything fails
            _readQueue.clear()
            return
        }

        onConnectionStateChange(gatt, domainState)

        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> gatt?.requestMtu(BLEConstants.REQUESTED_MTU)
            BluetoothGatt.STATE_DISCONNECTED -> {
                // clear the queue if anything fails
                _readQueue.clear()
                gatt?.close()
            }

            else -> {}
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT DISCOVER SERVICES" }
            onGAttFailed("Cannot discover services")
            return
        }
        val service = gatt?.services
            ?.find { it.uuid.toKotlinUuid() == BLEConstants.DEVICE_INFO_SERVICE_ID } ?: run {
            gatt?.disconnect()
            gatt?.close()
            Logger.w(TAG) { "INVALID CHARACTERISTICS FOUND CLOSING CONNECTION " }
            return
        }
        // we read all the characteristics
        val requiredCharacteristics = service.characteristics.filter { it.uuid != null }
        Logger.d(TAG) { "NO. OF CHARACTERISTICS FOUND STARTING READ :${requiredCharacteristics.size}" }
        // read characteristic
        _readQueue.addAll(requiredCharacteristics)
        _readQueue.poll()?.let {
            Logger.d(TAG) { "REQUESTING ${it.uuid} READ" }
            gatt.readCharacteristic(it)
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "CANNOT DISCOVER SERVICES" }
            onGAttFailed("Cannot request higher mtu")
            return
        }
        Logger.d(TAG) { "UPDATED MTU :$mtu" }
        gatt?.discoverServices()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        onCharacteristicRead(gatt, characteristic, characteristic.value, status)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            onGAttFailed("Cannot read characteristics :${characteristic.uuid}")
            Logger.w(TAG) { "CANNOT READ CHARACTERISTICS" }
            return
        }
        coroutineScope.launch {
            Logger.d(TAG) { "READING CHARACTERISTICS :${characteristic.uuid}" }
            onReadCharacteristic(gatt, characteristic.uuid.toKotlinUuid(), value)
            // check if anything left on the queue
            _readQueue.poll()?.let { gatt.readCharacteristic(it) }
        }
    }


    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Logger.w(TAG) { "WRITE CHARACTERISTICS FAILED" }
            return
        }
        coroutineScope.launch {
            Logger.d(TAG) { "WRITE CHARACTERISTICS FOR :${characteristic.uuid} SUCCESS" }
            onWriteCharacteristic(gatt, characteristic.uuid.toKotlinUuid())
        }
    }

    fun cleanUp() {
        _readQueue.clear()
    }
}