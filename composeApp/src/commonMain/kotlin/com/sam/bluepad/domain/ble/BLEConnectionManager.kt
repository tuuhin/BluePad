package com.sam.bluepad.domain.ble

import com.sam.bluepad.domain.ble.enums.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface BLEConnectionManager {

	/**
	 * A flow that emits the current connection state to the BLE device.
	 */
	val connectionState: Flow<BLEConnectionState>

	/**
	 * Establishes a connection to a BLE device, performs service discovery, and retrieves data.
	 *
	 * @param address The MAC address of the BLE device to connect to.
	 * @param informReceiver A flag to indicate whether to inform the advertiser on reading the device info
	 *@param disconnectOnDone Whether to close the connection when the information exchange is done
	 */
	fun connectAndReceiveData(
		address: String,
		informReceiver: Boolean = true,
		disconnectOnDone: Boolean = true,
	): Flow<Resource<BLEPeerData, Exception>>

	/**
	 * Disconnects from the currently connected device and releases all resources.
	 */
	suspend fun disconnect()

	/**
	 * Clears the resources
	 */
	fun cleanUp()
}