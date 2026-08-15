package com.sam.bluepad.data.ble.exceptions

import com.sam.bluepad.data.sync.dto.BLEHandshakeFailedReason

sealed class BLEConnectorException(message: String) : Exception(message) {

    class InvalidSessionTypeException :
        BLEConnectorException("Provided session type is invalid or any handler is not present")

    class SyncStarkNotAckException : BLEConnectorException("Start is not ack properly missing ack flag")
    class InvalidHandshakeValueException : BLEConnectorException("Invalid Handshake value")
    class InvalidAcknowledgementException(reason: BLEHandshakeFailedReason) :
        BLEConnectorException("Invalid Acknowledgement :${reason.name}")

    class InvalidPayloadDataException : BLEConnectorException("Invalid payload type its not supported")

    class SyncFlagMissingException : BLEConnectorException("No sync flag found in the read response")
    class LocalDeviceInfoMissing : BLEConnectorException("Local device data need to be known")
}

