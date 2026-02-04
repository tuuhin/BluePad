package com.sam.bluepad.domain.ble.models

import com.sam.bluepad.domain.models.ExternalDeviceModel
import kotlin.uuid.Uuid

sealed interface BLEDeviceSyncEvent {
    /**
     * Process of scanning for nearby BLE devices has started.
     * This is the initial event in the discovery phase.
     */
    data object DiscoveryStarted : BLEDeviceSyncEvent

    /**
     * A potential BLE device has been discovered during a scan.
     *
     * @property identifier A unique identifier for the discovered device,
     * such as its MAC address.
     */
    data class DeviceFound(val identifier: String) : BLEDeviceSyncEvent

    /**
     * No devices found in the given scan duration
     */
    data object DeviceScanTimeout : BLEDeviceSyncEvent

    /**
     * A connection to the target BLE device has been successfully established, but the services are
     * yet to be known
     */
    data object ConnectionSuccess : BLEDeviceSyncEvent

    /**
     * The services and characteristics of the connected BLE device
     * have been successfully discovered and are ready for interaction.
     */
    data object ServicesDiscovered : BLEDeviceSyncEvent

    /**
     * Successful reading of the main advertising data from a specific
     * BLE characteristic.
     *
     * @property characteristicsId The UUID of the characteristic from which the data was read.
     * @property device The parsed advertising data payload.
     * @see BLESyncData.BLEAdvertiseData
     */
    data class AdvertisingDataRead(
        val characteristicsId: Uuid,
        val device: ExternalDeviceModel
    ) : BLEDeviceSyncEvent

    /**
     * An advertising data has been successfully written back to the peripheral device.
     * This event indicates the app has sent its part of the data exchange.
     */
    data object AdvertisingResponseSend : BLEDeviceSyncEvent

    /**
     * A final acknowledgment from the peripheral.
     * This typically concludes the data sync logic.
     *
     * @property ack The acknowledgment data received from the device,
     * which may indicate the success or failure of the sync on its end.
     * @see BLESyncData.BLESyncACKSuccess
     */
    data class AdvertisingAcknowledgmentReceived(
        val ack: BLESyncData.BLESyncACKSuccess
    ) : BLEDeviceSyncEvent

    /**
     * THe device has disconnected, either intentionally by the app or
     * unexpectedly (e.g., out of range).
     */
    data object DeviceDisconnected : BLEDeviceSyncEvent
}