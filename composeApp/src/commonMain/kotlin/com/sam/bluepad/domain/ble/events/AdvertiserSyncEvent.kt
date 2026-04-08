package com.sam.bluepad.domain.ble.events

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface AdvertiserSyncEvent {

    data object IncomingHandshakeRequest : AdvertiserSyncEvent
    data class HandshakeSuccess(val device: ExternalDeviceModel) : AdvertiserSyncEvent
    data class HandshakeFailed(val message: String) : AdvertiserSyncEvent

    data class SyncStarted(val device: ExternalDeviceModel) : AdvertiserSyncEvent
    data class SyncCompleted(val device: ExternalDeviceModel, val isFull: Boolean = false) : AdvertiserSyncEvent
    data class SyncFailed(val reason: String) : AdvertiserSyncEvent
}
