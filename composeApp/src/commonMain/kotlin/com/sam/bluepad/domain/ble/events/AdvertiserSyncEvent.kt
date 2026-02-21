package com.sam.bluepad.domain.ble.events

import com.sam.bluepad.domain.models.ExternalDeviceModel

sealed interface AdvertiserSyncEvent {

    /**
     * Indicate an external device is trying to initiate a sync connection with the device
     */
    data class ForeignSyncRequest(val device: ExternalDeviceModel) : AdvertiserSyncEvent
}