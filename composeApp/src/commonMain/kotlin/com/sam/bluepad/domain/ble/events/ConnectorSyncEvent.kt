package com.sam.bluepad.domain.ble.events

import com.sam.bluepad.domain.models.ExternalDeviceModel

/**
 * Events generated during the synchronization process from the connector's (client) perspective.
 */
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
     * Handshake successfully completed, confirming device compatibility and identity.
     * @property device The remote device that we have successful handshake.
     */
    data class HandshakeSuccess(val device: ExternalDeviceModel) : ConnectorSyncEvent

    /**
     * Handshake process failed.
     * @property message Error message describing the failure.
     */
    data class HandshakeFailed(val message: String? = null) : ConnectorSyncEvent

    /**
     * Synchronization phase has started.
     * @property device The device with which synchronization is occurring.
     */
    data class SyncStarted(val device: ExternalDeviceModel) : ConnectorSyncEvent

    /**
     * Half duplex synchronization (one-way data transfer) completed successfully.
     * @property device The device synced with.
     */
    data class HalfDuplexCompleted(val device: ExternalDeviceModel) : ConnectorSyncEvent

    /**
     * Full duplex synchronization (two-way data transfer) completed successfully.
     * @property device The device synced with.
     * @property sessionId The unique identifier for this synchronization session.
     */
    data class FullDuplexCompleted(val device: ExternalDeviceModel, val sessionId: kotlin.uuid.Uuid) : ConnectorSyncEvent

    /**
     * Synchronization process failed.
     * @property reason Reason for the synchronization failure.
     */
    data class SyncFailed(val reason: String) : ConnectorSyncEvent

    /**
     * The remote device is currently processing data (e.g., database operations).
     */
    data object RemoteProcessing : ConnectorSyncEvent

    /**
     * The device has disconnected, either intentionally by the app or
     * unexpectedly (e.g., out of range).
     */
    data object DeviceDisconnected : ConnectorSyncEvent
}
