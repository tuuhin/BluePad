package com.sam.bluepad.data.bluetooth

import co.touchlab.kermit.Logger
import com.sam.bluepad.data.utils.PlatformDispatcherProvider
import com.sam.bluepad.domain.bluetooth.BTDeviceBondManager
import com.sam.bluepad.domain.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.domain.bluetooth.models.BTDeviceBondInfo
import com.sam.bluepad.domain.exceptions.BluetoothInvalidAddressException
import com.sam.bluepad.domain.exceptions.BluetoothInvalidBondRequest
import com.sam.bluepad.domain.exceptions.BluetoothInvalidDeviceException
import com.sam.bt_common.models.BTJVMBondResult
import com.sam.bt_common.models.BTJVMBondState
import com.sam.bt_common.models.BTJVMCreateBondError
import com.sam.bt_common.platform.PlatformBondInfoProvider
import com.sam.bt_common.readBondStateAsync
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

private const val TAG = "BLUETOOTH_DEVICE_BOND_MANAGER"

actual class BTDeviceBondManagerImpl(
    private val platformDispatcherProvider: PlatformDispatcherProvider
) : BTDeviceBondManager {

    override val isFeatureAvailable: Boolean
        get() = PlatformBondInfoProvider().use { it.canReadBondInfo }

    override val canShowConfirmPinDialog: Boolean
        get() = PlatformBondInfoProvider().use { it.canShowConfirmPinDialog }

    override suspend fun checkBondState(address: String): Result<BTDeviceBondState> {
        return runCatching {
            val jvmResult = PlatformBondInfoProvider()
                .use { provider -> provider.readBondStateAsync(address) }

            Logger.d(tag = TAG) { "BLUETOOTH BOND STATE FOR DEVICE (ADDRESS: $address) $jvmResult" }
            when (jvmResult) {
                BTJVMBondState.DEVICE_BONDED -> BTDeviceBondState.BONDED
                BTJVMBondState.DEVICE_NOT_BONDED -> BTDeviceBondState.NOT_BONDED
                BTJVMBondState.ERROR_INVALID_DEVICE -> throw BluetoothInvalidAddressException()
                BTJVMBondState.ERROR_DEVICE_CANNOT_PAIR -> throw BluetoothInvalidDeviceException()
                BTJVMBondState.ERROR_UNKNOWN -> throw IllegalStateException("Invalid bond state")
            }
        }
    }

    override fun requestBond(address: String): Flow<BTDeviceBondInfo> {
        return callbackFlow {

            val bondManager = PlatformBondInfoProvider()

            // try to get the flow if any error throws the error cancelling the flow
            val currentState = checkBondState(address)
                .getOrThrow()

            Logger.d(tag = TAG) { "BLUETOOTH CURRENT BOND STATE STATE FOR DEVICE :$address $currentState" }
            send(BTDeviceBondInfo.BondState(currentState))

            // only continue the flow if the device is not  bonded
            if (currentState != BTDeviceBondState.NOT_BONDED) throw BluetoothInvalidBondRequest(address)

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
                        BTJVMBondResult.ALREADY_PAIRED -> throw BluetoothInvalidBondRequest(address)
                        // custom exception based on the bond result
                        else -> throw BTBondException(response.toGeneralErrorMessage ?: "")
                    }

                },
                onError = { code ->
                    Logger.e(tag = TAG) { "UNABLE TO PERFORM BOND ERROR CODE:$code" }
                    val code = BTJVMCreateBondError.fromInt(code)
                    close(IllegalStateException(" UNABLE TO BOND TO THE DEVICE :$code"))
                },
            )

            awaitClose {
                Logger.d(tag = TAG) { "BLUETOOTH BOND MANAGER UNREGISTERED" }
                bondManager.unregisterForBondConfirmPin()
            }

        }.flowOn(platformDispatcherProvider.io)
    }


    private class BTBondException(override val message: String) : Exception(message)

    private val BTJVMBondResult.toGeneralErrorMessage: String?
        get() = when (this) {
            BTJVMBondResult.NOT_READY_TO_PAIR -> "Device not ready"
            BTJVMBondResult.CONNECTION_REJECTED -> "Device rejected connection"
            BTJVMBondResult.TOO_MANY_CONNECTION -> "Too many devices trying to connect"
            BTJVMBondResult.HARDWARE_FAILURE -> "Hardware is unable to connect to the other device"
            BTJVMBondResult.AUTHENTICATION_TIMEOUT -> "Authentication timeout"
            BTJVMBondResult.AUTHENTICATION_NOT_ALLOWED -> "Authentication not allowed"
            BTJVMBondResult.AUTHENTICATION_FAILURE -> "Authentication failed"
            BTJVMBondResult.NO_SUPPORTED_PROFILES -> "Bluetooth profile not supported"
            BTJVMBondResult.PROTECTION_LEVEL_ISSUES, BTJVMBondResult.ACCESS_DENIED -> "Bluetooth access denied"
            BTJVMBondResult.PARING_OPERATION_CANCELLED, BTJVMBondResult.ERROR_OPERATION_CANCELLED -> "Paring cancelled"
            BTJVMBondResult.INVALID_DATA -> "Invalids result"
            BTJVMBondResult.HANDLER_NOT_REGISTERED -> "Pairing receiver not found (dev)"
            BTJVMBondResult.REJECTED_BY_HANDLER -> "Pairing rejected by handler"
            BTJVMBondResult.FAILED, BTJVMBondResult.ERROR_UNKNOWN -> "Failed to receive bond data"
            BTJVMBondResult.OPERATION_IN_PROGRESS -> "Operation in progress"
            else -> null
        }

}
