package com.sam.bluepad.domain.bluetooth

import com.sam.bluepad.domain.bluetooth.enums.BTDeviceBondState
import com.sam.bluepad.domain.bluetooth.models.BTDeviceBondInfo
import kotlinx.coroutines.flow.Flow

/**
 * Manages the bonding (pairing) lifecycle and security authentication
 * for Bluetooth devices.
 */
interface BTDeviceBondManager {

    /**
     * Indicates whether Bluetooth bonding capabilities are supported and
     * enabled on the current hardware and OS platform.
     */
    val isFeatureAvailable: Boolean

    /**
     * Determines if the system supports to show a custom dialog
     * to display a PIN confirmation dialog to the user.
     */
    val canShowConfirmPinDialog: Boolean

    /**
     * Queries the current bonding state of a specific remote Bluetooth device.
     *
     * @param address The MAC address or unique hardware identifier of the target device.
     * @return A [Result] wrapping the current [BTDeviceBondState]. Returns a failure
     * if the address is invalid or the internal hardware query fails.
     */
    suspend fun checkBondState(address: String): Result<BTDeviceBondState>

    /**
     * Initiates a bonding request with a remote device and returns a cold stream
     * of status updates.
     * @param address The MAC address or unique hardware identifier of the device to pair with.
     * @return A [Flow] emitting [BTDeviceBondInfo] updates throughout the lifecycle of the request.
     */
    fun requestBond(address: String): Flow<BTDeviceBondInfo>

    /**
     * Submits a user-entered or system-provided PIN code to authenticate and
     * complete an ongoing secure bonding sequence.
     *
     * @param pin The numeric or alphanumeric PIN string used to confirm the pairing request.
     * @return A [Result] indicating whether the PIN submission was successfully
     * accepted by the local Bluetooth subsystem.
     * * *Info:* A successful result does not guarantee a successful bond use the [checkBondState] for outcome
     */
    suspend fun acceptBondConfirmationPin(pin: String): Result<Unit>
}
