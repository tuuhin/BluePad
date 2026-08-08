@file:SuppressLint("MissingPermission")

package com.sam.bluepad.bluetooth.data

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
import com.sam.bluepad.bluetooth.exceptions.BluetoothInvalidAddressException
import com.sam.bluepad.bluetooth.exceptions.BluetoothInvalidBondRequest
import com.sam.bluepad.bluetooth.exceptions.BluetoothInvalidDeviceException
import com.sam.bluepad.common.utils.ICoroutineDispatchersProvider
import com.sam.bluepad.domain.bluetooth.IBTDeviceBondManager
import com.sam.bluepad.model.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.model.bluetooth.models.BTDeviceBondInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private const val TAG = "BLUETOOTH_DEVICE_BOND_MANAGER"

@Single(binds = [IBTDeviceBondManager::class])
internal actual class BTDeviceBondManager(
    private val context: Context,
    private val platformDispatchers: ICoroutineDispatchersProvider
) : IBTDeviceBondManager {


    private val bluetoothManager by lazy { context.getSystemService<BluetoothManager>() }

    actual override val isFeatureAvailable: Boolean = true
    actual override val canShowConfirmPinDialog: Boolean = false

    actual override suspend fun checkBondState(address: String): Result<BTDeviceBondState> = runCatching {
        val isAddressOK = BluetoothAdapter.checkBluetoothAddress(address)
        if (!isAddressOK) throw BluetoothInvalidAddressException()
        // read the bluetooth device state on io dispatcher
        val device = withContext(platformDispatchers.io) {
            bluetoothManager?.adapter?.getRemoteDevice(address) ?: throw BluetoothInvalidDeviceException()
        }
        device.bondState.toBondState()
    }

    @SuppressLint("MissingPermission")
    actual override fun requestBond(address: String): Flow<BTDeviceBondInfo> {
        return channelFlow {
            // create and register a receiver and wait for values
            val receiver = object : BroadcastReceiver() {

                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return

                    val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                    val current = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

                    if (prev == -1 || current == -1) return

                    @Suppress("DEPRECATION")
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

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
            val device = bluetoothManager?.adapter?.getRemoteDevice(address) ?: run {
                close(BluetoothInvalidDeviceException())
                return@channelFlow
            }

            val bondState = device.bondState.toBondState()
            if (bondState != BTDeviceBondState.NOT_BONDED) {
                close(BluetoothInvalidBondRequest(address))
            }

            // only create bond if the device is not bonded
            Logger.d(tag = TAG) { "REQUESTING CREATE BOND FOR DEVICE:${device.address}" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                device.createBond(BluetoothDevice.TRANSPORT_LE)
            } else {
                device.createBond()
            }
            Logger.d(tag = TAG) { "REQUESTED CREATE BOND FOR DEVICE:${device.address}" }

            awaitClose {
                Logger.d(tag = TAG) { "UNREGISTERING RECEIVER FOR BOND STATE " }
                context.unregisterReceiver(receiver)
            }
        }.flowOn(platformDispatchers.io)
    }

    actual override suspend fun acceptBondConfirmationPin(pin: String): Result<Unit> =
        Result.failure(IllegalStateException("Platform don't support custom pin dialogs"))

    private fun Int.toBondState(): BTDeviceBondState = when (this) {
        BluetoothDevice.BOND_BONDING -> BTDeviceBondState.BONDING
        BluetoothDevice.BOND_BONDED -> BTDeviceBondState.BONDED
        BluetoothDevice.BOND_NONE -> BTDeviceBondState.NOT_BONDED
        else -> throw IllegalStateException("Invalid bluetooth bond state")
    }
}
