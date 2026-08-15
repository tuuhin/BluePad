package com.sam.bluepad.data.ble.exceptions

import com.sam.bluepad.data.sync.dto.BLESyncSession
import kotlin.uuid.Uuid

sealed class BLEAdvertiserException(message: String) : Exception(message) {

    class UnsupportedSyncSessionException(session: BLESyncSession) :
        BLEAdvertiserException("The sync session request of type ${session::class.simpleName} is not supported or was received out of sequence")

    class LocalIdentityMissingException :
        BLEAdvertiserException("Local device information is required for BLE identification but was not provided.")

    class UnrecognizedPeerDeviceException(val deviceId: Uuid) :
        BLEAdvertiserException("Sync denied: The device $deviceId is not in the list of authorized or saved devices.")

    class InvalidSyncPayloadException :
        BLEAdvertiserException("The synchronization data was processed successfully, but the resulting payload state is invalid for current transaction")
}
