package com.sam.bluepad.domain.ble.events

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface AdvertiserSyncEvent {

    data class HandshakeSuccess(val device: ExternalDeviceModel) : AdvertiserSyncEvent
    data class HandshakeFailed(val message: String) : AdvertiserSyncEvent
    data class SyncStarted(val device: ExternalDeviceModel) : AdvertiserSyncEvent
    data class SyncCompleted(val device: ExternalDeviceModel) : AdvertiserSyncEvent
    data class SyncFailed(val device: ExternalDeviceModel, val reason: String) : AdvertiserSyncEvent
}
