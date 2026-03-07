package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Represents the state machine and message protocol for a Bluetooth Low Energy (BLE)
 * synchronization session between a peripheral and a central device.
 */
@Serializable
@SerialName("_bss")
sealed class BLESyncSession {

    /**
     * The initial handshake request sent to initiate a synchronization session.
     */
    @Serializable
    @SerialName("sss")
    data object SyncSessionStart : BLESyncSession()

    /**
     * The response to a [SyncSessionStart] request.
     * @property isAck True if the session can proceed; false if the device is busy or rejected.
     */
    @Serializable
    @SerialName("sssa")
    data class SyncSessionStartAck(val isAck: Boolean) : BLESyncSession()

    /**
     * A discrete packet of synchronization data.
     * @property type The category or schema of the data being sent.
     * @property sequenceNumber Used to ensure packets are processed in the correct order.
     * @property payload The serialized data content.
     * @see BLESyncDataType
     */
    @Serializable
    @SerialName("bsdp")
    data class BLESyncDataPacket(
        @ProtoNumber(1) val type: BLESyncDataType,
        @ProtoNumber(2) val sequenceNumber: Int,
        @ProtoNumber(3) val payload: String
    ) : BLESyncSession()

    /**
     * Acknowledgment for a [BLESyncDataPacket] to confirm successful reception.
     * @property type Matches the type of the packet being acknowledged.
     * @property sequenceNumber Matches the sequence number of the received packet.
     */
    @Serializable
    @SerialName("bsda")
    data class BLESyncDataAck(
        @ProtoNumber(1) val type: BLESyncDataType,
        @ProtoNumber(2) val sequenceNumber: Int,
    ) : BLESyncSession()


    /**
     * Coordinates transitions between different data types or sync phases.
     * @property prevType The data type the session is moving away from.
     * @property newType The data type the session is moving toward.
     * @property isRequested True if this is an initiation of a switch.
     * @property isAck True if this confirms the switch is ready.
     */
    @Serializable
    @SerialName("spt")
    data class SyncPacketTransition(
        @ProtoNumber(1) val prevType: BLESyncDataType? = null,
        @ProtoNumber(2) val newType: BLESyncDataType? = null,
        @ProtoNumber(3) val isRequested: Boolean = true,
        @ProtoNumber(4) val isAck: Boolean = false,
    ) : BLESyncSession()


    /**
     * Signifies that the synchronization session has finished successfully.
     */
    @Serializable
    @SerialName("ssc")
    data object SyncSessionCompleted : BLESyncSession()


    /**
     * Sent when the session must terminate due to an error.
     * @property reason The specific error that caused the failure.
     */
    @Serializable
    @SerialName("ssf")
    data class SyncSessionFailed(
        @ProtoNumber(1) val reason: BLESyncFailedReason
    ) : BLESyncSession()

}