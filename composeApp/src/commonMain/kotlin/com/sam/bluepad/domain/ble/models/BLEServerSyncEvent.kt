package com.sam.bluepad.domain.ble.models

import com.sam.bluepad.domain.models.ExternalDeviceModel
import kotlin.uuid.Uuid

sealed interface BLEServerSyncEvent {

    data class SyncRequest(
        val device: ExternalDeviceModel,
        val connectorUuid: Uuid
    ) : BLEServerSyncEvent
}
