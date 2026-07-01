package com.sam.bluepad.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.bluetooth.BTDeviceBondManager
import com.sam.bluepad.domain.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.domain.bluetooth.models.BTDeviceBondInfo
import com.sam.bluepad.domain.exceptions.BluetoothInvalidAddressException
import com.sam.bluepad.domain.exceptions.BluetoothInvalidBondRequest
import com.sam.bluepad.domain.exceptions.BluetoothInvalidDeviceException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val TAG = "BluetoothBondManager"

@SuppressLint("MissingPermission")
actual class BTDeviceBondManagerImpl(
    private val context: Context,
    private val platformDispatchers: PlatformDispatcherProvider
) : BTDeviceBondManager {

    private val _btManager by lazy { context.getSystemService<BluetoothManager>() }

    override val isFeatureAvailable: Boolean = true
    override val canShowConfirmPinDialog: Boolean = false

    override suspend fun checkBondState(address: String): Result<BTDeviceBondState> {
        return runCatching {
            val isAddressOK = BluetoothAdapter.checkBluetoothAddress(address)
            if (!isAddressOK) throw BluetoothInvalidAddressException()
            // read the bluetooth device state on io dispatcher
            val device = withContext(platformDispatchers.io) {
                _btManager?.adapter?.getRemoteDevice(address) ?: throw BluetoothInvalidDeviceException()
            }
            device.bondState.toBondState()
        }
    }

    override fun requestBond(address: String): Flow<BTDeviceBondInfo> {
        return channelFlow {
            // create and register a receiver and wait for values
            val receiver = object : BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return

                    val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    val current = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

                    if (prev == -1 || current == -1) return

                    @Suppress("DEPRECATION")
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    Logger.d(tag = TAG) { " DEVICE :${device?.address} BOND STATE CHANGED :$current" }
                    trySend(BTDeviceBondInfo.BondState(current.toBondState()))

                    // successfully bonded the device can close the flow now
                    if (prev == BluetoothDevice.BOND_BONDING && current == BluetoothDevice.BOND_BONDED) close()
                }
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED,
            )

            // check the address
            val isAddressOK = BluetoothAdapter.checkBluetoothAddress(address)
            if (!isAddressOK) close(BluetoothInvalidAddressException())

            // fetch the address
            val device = _btManager?.adapter?.getRemoteDevice(address) ?: run {
                close(BluetoothInvalidDeviceException())
                return@channelFlow
            }

            val bondState = device.bondState.toBondState()
            if (bondState != BTDeviceBondState.NOT_BONDED)
                close(BluetoothInvalidBondRequest(address))

            // only create bond if the device is not bonded
            Logger.d(tag = TAG) { "REQUESTING CREATE BOND FOR DEVICE:${device.address}" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN)
                device.createBond(BluetoothDevice.TRANSPORT_LE)
            else device.createBond()
            Logger.d(tag = TAG) { "REQUESTED CREATE BOND FOR DEVICE:${device.address}" }

            awaitClose {
                Logger.d(tag = TAG) { "UNREGISTERING RECEIVER FOR BOND STATE " }
                context.unregisterReceiver(receiver)
            }
        }.flowOn(platformDispatchers.io)
    }

    override suspend fun acceptBondConfirmationPin(pin: String): Result<Unit> =
        Result.failure(IllegalStateException("Platform don't support custom pin dialogs"))

    private fun Int.toBondState(): BTDeviceBondState {
        return when (this) {
            BluetoothDevice.BOND_BONDING -> BTDeviceBondState.BONDING
            BluetoothDevice.BOND_BONDED -> BTDeviceBondState.BONDED
            BluetoothDevice.BOND_NONE -> BTDeviceBondState.NOT_BONDED
            else -> throw IllegalStateException("Invalid bluetooth bond state")
        }
    }
}
