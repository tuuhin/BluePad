package com.sam.bluepad.bluetooth.data

import co.touchlab.kermit.Logger
import com.sam.bluepad.bluetooth.data.ext.description
import com.sam.bluepad.bluetooth.exceptions.BluetoothBondException
import com.sam.bluepad.bluetooth.exceptions.BluetoothInvalidAddressException
import com.sam.bluepad.bluetooth.exceptions.BluetoothInvalidBondRequest
import com.sam.bluepad.common.utils.ICoroutineDispatchersProvider
import com.sam.bluepad.domain.bluetooth.IBTDeviceBondManager
import com.sam.bluepad.model.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.model.bluetooth.models.BTDeviceBondInfo
import com.sam.bt_common.models.BTJVMBondResult
import com.sam.bt_common.models.BTJVMBondState
import com.sam.bt_common.models.BTJVMCreateBondError
import com.sam.bt_common.platform.PlatformBondInfoProvider
import com.sam.bt_common.readBondStateAsync
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private const val TAG = "BLUETOOTH_BOND_MANAGER"

@Single(binds = [IBTDeviceBondManager::class])
internal actual class BTDeviceBondManager(
    private val dispatcher: ICoroutineDispatchersProvider
) : IBTDeviceBondManager {


    /**
     * A common platform bond provider instance as request and accept need to be performed by a single instance
     * otherwise having some issues with conflicts
     */
    private var mBondInfoProvider: PlatformBondInfoProvider? = null

    actual override val isFeatureAvailable: Boolean
        get() = PlatformBondInfoProvider().use { it.canReadBondInfo }

    actual override val canShowConfirmPinDialog: Boolean
        get() = PlatformBondInfoProvider().use { it.canShowConfirmPinDialog }

    actual override suspend fun checkBondState(address: String): Result<BTDeviceBondState> = runCatching {
        val jvmResult = PlatformBondInfoProvider()
            .use { provider -> provider.readBondStateAsync(address) }

        Logger.d(tag = TAG) { "BLUETOOTH BOND STATE FOR DEVICE (ADDRESS: $address) $jvmResult" }
        when (jvmResult) {
            BTJVMBondState.DEVICE_BONDED -> {
                BTDeviceBondState.BONDED
            }

            BTJVMBondState.DEVICE_NOT_BONDED -> {
                BTDeviceBondState.NOT_BONDED
            }

            BTJVMBondState.ERROR_INVALID_DEVICE, BTJVMBondState.ERROR_DEVICE_CANNOT_PAIR -> {
                throw BluetoothInvalidAddressException()
            }

            BTJVMBondState.ERROR_UNKNOWN -> {
                throw IllegalStateException("Invalid bond state")
            }
        }
    }

    actual override fun requestBond(address: String): Flow<BTDeviceBondInfo> = callbackFlow {
        val bondManager = PlatformBondInfoProvider()

        // close the previous one and then only set the new
        mBondInfoProvider?.close()
        mBondInfoProvider = bondManager

        // try to get the flow if any error throws the error cancelling the flow
        val currentState = checkBondState(address)
            .getOrThrow()

        Logger.d(tag = TAG) { "BLUETOOTH CURRENT BOND STATE STATE FOR DEVICE :$address $currentState" }

        // only continue the flow if the device is not  bonded
        if (currentState != BTDeviceBondState.NOT_BONDED) {
            close(BluetoothInvalidBondRequest(address))
        }

        Logger.d(tag = TAG) { "BLUETOOTH BOND MANAGER REGISTERING" }
        bondManager.registerForBondConfirmPin(
            address = address,
            onConfirmPin = { pin ->
                Logger.d(tag = TAG) { "RECEIVED CONFIRMATION PIN :$pin" }
                trySend(BTDeviceBondInfo.ConfirmPin(pin))
            },
            onResponse = { code ->
                val response = BTJVMBondResult.fromInt(code)
                Logger.d(tag = TAG) { "RECEIVED FINAL RESPONSE :$response" }
                when (response) {
                    BTJVMBondResult.BONDED -> trySend(BTDeviceBondInfo.BondState(BTDeviceBondState.BONDED))

                    BTJVMBondResult.ALREADY_PAIRED -> close(BluetoothInvalidBondRequest(address))

                    // custom exception based on the bond result
                    else -> close(BluetoothBondException(response.description ?: ""))
                }
            },
            onError = { code ->
                Logger.e(tag = TAG) { "UNABLE TO PERFORM BOND ERROR CODE:$code" }
                val code = BTJVMCreateBondError.fromInt(code)
                close(BluetoothBondException(" UNABLE TO BOND TO THE DEVICE :$code"))
            },
        )

        awaitClose {
            Logger.d(tag = TAG) { "BLUETOOTH BOND MANAGER UNREGISTERED" }
            bondManager.unregisterForBondConfirmPin()
            mBondInfoProvider?.close()
            mBondInfoProvider = null
        }
    }.flowOn(dispatcher.io)

    actual override suspend fun acceptBondConfirmationPin(pin: String): Result<Unit> = runCatching {
        withContext(dispatcher.io) {
            val provider = mBondInfoProvider
                ?: throw BluetoothBondException("Cannot accept pin until a request bond is being made")
            provider.acceptConfirmPin(pin)
        }
    }
}
