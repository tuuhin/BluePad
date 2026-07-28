package com.sam.bluepad.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattConnectionSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.ble.callbacks.DeviceConnectionCallback
import com.sam.bluepad.data.utils.PlatformInfoProvider
import com.sam.bluepad.data.utils.hasBLEScanPermission
import com.sam.bluepad.domain.ble.BLEConnectionManager
import com.sam.bluepad.domain.ble.BLEConstants
import com.sam.bluepad.domain.ble.enums.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.exceptions.BLEConnectionFailedException
import com.sam.bluepad.domain.exceptions.BluetoothInvalidAddressException
import com.sam.bluepad.domain.exceptions.BluetoothInvalidDeviceException
import com.sam.bluepad.domain.exceptions.BluetoothNotEnabledException
import com.sam.bluepad.domain.exceptions.BluetoothPermissionException
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.use_cases.RandomGenerator
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.uuid.toKotlinUuid

private const val TAG = "BLE_CONNECTOR"

@SuppressLint("MissingPermission")
actual class BLEConnectionManagerImpl(
    private val context: Context,
    private val protoBuf: ProtoBuf,
    private val deviceInfoProvider: LocalDeviceInfoProvider,
    private val platformInfoProvider: PlatformInfoProvider,
    private val randomGenerator: RandomGenerator,
) : BLEConnectionManager {

    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

    private val _btAdapter: BluetoothAdapter?
        get() = _bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(BLEConnectionState.DISCONNECTED)
    override val connectionState: Flow<BLEConnectionState>
        get() = _connectionState.asStateFlow()

    private var _gattConnection: BluetoothGatt? = null

    private val _localDeviceInfo = deviceInfoProvider.readDeviceInfo.stateIn(
        scope = _scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    override fun connectAndReceiveData(
        address: String,
        informReceiver: Boolean,
        disconnectOnDone: Boolean
    ): Flow<Resource<BLEPeerData, Exception>> = callbackFlow {

        trySend(Resource.Loading)

        // TODO: SOME OF THE EDGE CASES ARE NOT HANDLED
        val callback = DeviceConnectionCallback(
            coroutineScope = _scope,
            onConnectionStateChange = { _, state ->
                val updated = _connectionState.updateAndGet { state }
                if (updated == BLEConnectionState.DISCONNECTED) {
                    // close the connection and close the flow
                    cleanUp()
                    close()
                }
            },
            onGAttFailed = { message ->
                trySend(Resource.Error(BLEConnectionFailedException(message)))
                _connectionState.update { BLEConnectionState.DISCONNECTED }
                close()
            },
            onWriteCharacteristic = { gatt, uuid ->
                if (uuid == BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID && disconnectOnDone) {
                    gatt.disconnect()
                    close()
                }
            },
            onReadCharacteristic = { gatt, uuid, bytes ->
                when (uuid) {
                    BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID -> {
                        val peerData = try {
                            protoBuf.decodeFromByteArray<BLEPeerData>(bytes)
                        } catch (e: Exception) {
                            Logger.e(tag = TAG, throwable = e) { "UNABLE TO DECODE BYTES" }
                            trySend(Resource.Error(Exception("Cannot decode peer data")))
                            gatt.close()
                            return@DeviceConnectionCallback
                        }
                        Logger.d(tag = TAG) { "PEER DATA :$peerData" }
                        trySend(Resource.Success(peerData))
                        if (informReceiver) gatt.sendDeviceInfo(nonce = peerData.nonce)
                    }

                    else -> {}
                }
            },
        )

        // initiate connection
        val result = connectAndWaitForExchange(address, callback)
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            if (exception is Exception) trySend(Resource.Error(exception))
            // close the flow
            close()
        }

        // stop the connection if collector scope is cancelled
        awaitClose {
            val gatt = result.getOrNull()
            gatt?.close()
            callback.cleanUp()
            cleanUp()
        }
    }

    @Suppress("DEPRECATION")
    private fun BluetoothGatt.sendDeviceInfo(nonce: String? = null) {
        val characteristics = services
            .find { it.uuid.toKotlinUuid() == BLEConstants.DEVICE_INFO_SERVICE_ID }
            ?.characteristics
            ?.find { it.uuid.toKotlinUuid() == BLEConstants.DEVICE_INFO_CHARACTERISTICS_ID }
            ?: return

        val info = _localDeviceInfo.value ?: return
        val peerData = BLEPeerData(
            deviceId = info.deviceId,
            deviceName = info.name,
            deviceOs = platformInfoProvider.platformOS,
            nonce = nonce,
        )

        val bytes = try {
            protoBuf.encodeToByteArray<BLEPeerData>(peerData)
        } catch (e: Exception) {
            Logger.e(TAG, e) { "UNABLE TO SERIALIZE DEVICE INFO" }
            return
        }

        val isSuccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeCharacteristic(
                characteristics,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristics.value = bytes
            writeCharacteristic(characteristics)
        }
        Logger.d(tag = TAG) { "SENDING CURRENT DEVICE INFO STATUS:$isSuccess " }
    }

    private fun connectAndWaitForExchange(address: String, gattCallback: BluetoothGattCallback)
        : Result<BluetoothGatt?> {
        if (_bluetoothManager?.adapter?.isEnabled != true)
            return Result.failure(BluetoothNotEnabledException())
        if (!context.hasBLEScanPermission)
            return Result.failure(BluetoothPermissionException())

        if (!BluetoothAdapter.checkBluetoothAddress(address))
            return Result.failure(BluetoothInvalidAddressException())

        return try {
            val device = _btAdapter?.getRemoteDevice(address)
                ?: return Result.failure(BluetoothInvalidDeviceException())
            // connect to the gatt server
            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                val ioDispatcher = Dispatchers.IO
                    .limitedParallelism(2, "bluetooth_connection_executor")
                    .asExecutor()
                device.connectGatt(connectSettings, ioDispatcher, gattCallback)
            } else {
                @Suppress("DEPRECATION")
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
            Logger.d(tag = TAG) { "CLIENT GATT CONNECTION STARTED" }
            // return success if there is no error
            Result.success(gatt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        try {
            if (_gattConnection != null) {
                _gattConnection?.disconnect()
                Logger.d(tag = TAG) { "GATT CLIENT DISCONNECTED" }
                _connectionState.update { BLEConnectionState.DISCONNECTED }
            }
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "GATT CONNECTED FAILED TO CLOSE" }
        }
    }

    override fun cleanUp() {
        _scope.cancel()
        try {
            _gattConnection?.close()
            Logger.d(tag = TAG) { "GATT CLIENT CLOSED" }
            _gattConnection = null
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "GATT CONNECTED FAILED TO CLOSE" }
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
