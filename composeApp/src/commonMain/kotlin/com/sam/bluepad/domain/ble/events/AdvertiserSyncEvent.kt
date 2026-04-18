package com.sam.bluepad.domain.ble.events

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface AdvertiserSyncEvent {


    /**
     * Handshake process has started with a remote device.
     */
    data object HandshakeStarted : AdvertiserSyncEvent

    /**
     * Handshake successfully completed.
     * @property device The device we are now synced with.
     */
    data class HandshakeSuccess(val device: ExternalDeviceModel) : AdvertiserSyncEvent

    /**
     * Handshake failed.
     * @property message Error message.
     */
    data class HandshakeFailed(val message: String) : AdvertiserSyncEvent

    /**
     * Synchronization process started.
     */
    data class SyncStarted(val device: ExternalDeviceModel) : AdvertiserSyncEvent

    /**
     * Half duplex synchronization (one-way) completed successfully.
     */
    data class HalfDuplexCompleted(val device: ExternalDeviceModel) : AdvertiserSyncEvent

    /**
     * Full duplex synchronization (two-way) completed successfully.
     */
    data class FullDuplexCompleted(val device: ExternalDeviceModel) : AdvertiserSyncEvent

    /**
     * Synchronization failed.
     * @property reason Reason for failure.
     */
    data class SyncFailed(val reason: String) : AdvertiserSyncEvent
}
