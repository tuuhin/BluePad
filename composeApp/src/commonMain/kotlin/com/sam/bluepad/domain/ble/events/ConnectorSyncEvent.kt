package com.sam.bluepad.domain.ble.events

import com.sam.bluepad.data.sync.dto.BLESyncData.BLEAdvertiseData
import com.sam.bluepad.domain.ble.models.SyncDataExchangeStage
import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface ConnectorSyncEvent {

    /**
     * Process of scanning for nearby BLE devices has started.
     * This is the initial event in the discovery phase.
     */
    data object DiscoveryStarted : ConnectorSyncEvent

    /**
     * A potential BLE device has been discovered during a scan.
     *
     * @property identifier A unique identifier for the discovered device,
     * such as its MAC address.
     */
    data class DeviceFound(val identifier: String) : ConnectorSyncEvent

    /**
     * No devices found in the given scan duration
     */
    data object DeviceScanTimeout : ConnectorSyncEvent

    /**
     * A connection to the target BLE device has been successfully established, but the services are
     * yet to be known
     */
    data object ConnectionSuccess : ConnectorSyncEvent

    /**
     * The services and characteristics of the connected BLE device
     * have been successfully discovered and are ready for interaction.
     */
    data object ServicesDiscovered : ConnectorSyncEvent

    /**
     * Successful reading of the main advertising data from a specific
     * BLE characteristic.
     *
     * @property device The parsed advertising data payload.
     * @see BLEAdvertiseData
     */
    data class AdvertisingDeviceRead(val device: ExternalDeviceModel) : ConnectorSyncEvent

    /**
     * An advertising data has been successfully written back to the peripheral device.
     * This event indicates the app has sent its part of the data exchange.
     */
    data object ConnectorDeviceDataResponseSend : ConnectorSyncEvent

    /**
     * A final acknowledgment from the peripheral.
     * This typically concludes the proximity check logic.
     */
    data object AdvertisingAcknowledgmentReceived : ConnectorSyncEvent

    /**
     * Devices are exchanging some data
     * @property type Type of the data being exchanged
     * @see SyncDataExchangeStage
     */
    data class ExchangingData(val type: SyncDataExchangeStage) : ConnectorSyncEvent

    /**
     * Content exchange is acknowledged
     */
    data object ExchangeDataAck : ConnectorSyncEvent

    /**
     * THe device has disconnected, either intentionally by the app or
     * unexpectedly (e.g., out of range).
     */
    data object DeviceDisconnected : ConnectorSyncEvent
}