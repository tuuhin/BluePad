package com.sam.bluepad.data.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

/**
 * Represents the state machine and message protocol for a Bluetooth Low Energy (BLE)
 * synchronization session between a peripheral and a central device.
 *
 * ```mermaid
 * stateDiagram-v2
 *     [*] --> IDLE
 *     IDLE --> INITIATING : SyncSessionStart
 *     INITIATING --> READY : SyncSessionStartAck(isAck=true)
 *     INITIATING --> FAILED : SyncSessionStartAck(isAck=false)
 *
 *     state READY {
 *         [*] --> TRANSITIONING
 *         TRANSITIONING --> SYNCING : SyncPacketTransition(isAck=true)
 *         SYNCING --> SYNCING : BLESyncDataPacket / BLESyncDataAck
 *         SYNCING --> PROCESSING : BLESyncDataPacketEnd
 *         PROCESSING --> PROCESSING : SyncPacketProcessing
 *         PROCESSING --> TRANSITIONING : SyncPacketTransition(isRequested=true)
 *     }
 *
 *     READY --> COMPLETING : SyncSessionSuccessful
 *     COMPLETING --> FINISHED : SyncSessionSuccessfulAck
 *
 *     IDLE --> FAILED : SyncSessionFailed
 *     INITIATING --> FAILED : SyncSessionFailed
 *     READY --> FAILED : SyncSessionFailed
 *     COMPLETING --> FAILED : SyncSessionFailed
 *
 *     FAILED --> [*]
 *     FINISHED --> [*]
 * ```
 */
@Serializable
@SerialName("_bss")
sealed interface BLESyncSession {

    /**
     * Associated session id for the current session
     */
    val sessionId: Uuid

    /**
     * The initial handshake request sent to initiate a synchronization session.
     */
    @Serializable
    @SerialName("sss")
    data class SyncSessionStart(
        @ProtoNumber(1) override val sessionId: Uuid
    ) : BLESyncSession

    /**
     * The response to a [SyncSessionStart] request.
     * @property isAck True if the session can proceed; false if the device is busy or rejected.
     */
    @Serializable
    @SerialName("ss_sa")
    data class SyncSessionStartAck(
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val isAck: Boolean,
    ) : BLESyncSession

    /**
     * A discrete packet of synchronization data.
     * @property type The category or schema of the data being sent.
     * @property sequenceNumber Used to ensure packets are processed in the correct order.
     * @property payload The serialized data content.
     * @property isEmptyStart Specialized case for first empty output
     * @see BLESyncDataType
     */
    @Serializable
    @SerialName("bs_dp")
    data class BLESyncDataPacket(
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val type: BLESyncDataType,
        @ProtoNumber(3) val sequenceNumber: UInt = 0u,
        @ProtoNumber(4) val payload: String = "",
        @ProtoNumber(5) val isEmptyStart: Boolean = false,
    ) : BLESyncSession

    /**
     * A packet end marker
     * @property type  The category of packet that was being sent
     * @see BLESyncDataType
     */
    @Serializable
    @SerialName("bs_pe")
    data class BLESyncDataPacketEnd(
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val type: BLESyncDataType,
    ) : BLESyncSession

    /**
     * Data processing marker indicated data is being processed and will be
     * sending a result soon  enough
     */
    @Serializable
    @SerialName("spp")
    data class SyncPacketProcessing(
        @ProtoNumber(1) override val sessionId: Uuid
    ) : BLESyncSession

    /**
     * Acknowledgment for a [BLESyncDataPacket] to confirm successful reception.
     * @property type Matches the type of the packet being acknowledged.
     * @property sequenceNumber Matches the sequence number of the received packet.
     */
    @Serializable
    @SerialName("bs_da")
    data class BLESyncDataAck(
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val type: BLESyncDataType,
        @ProtoNumber(3) val sequenceNumber: UInt,
    ) : BLESyncSession


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
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val prevType: BLESyncDataType? = null,
        @ProtoNumber(3) val newType: BLESyncDataType,
        @ProtoNumber(4) val isRequested: Boolean = true,
        @ProtoNumber(5) val isAck: Boolean = false,
    ) : BLESyncSession


    /**
     * Signifies that the synchronization session has finished successfully.
     */
    @Serializable
    @SerialName("ssc")
    data class SyncSessionSuccessful(
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val reason: BLESyncCompletionReason = BLESyncCompletionReason.FULL_DUPLEX_SYNC_COMPLETED
    ) : BLESyncSession

    /**
     * When [SyncSessionSuccessful] is responded to the other party [BLESyncSession.SyncSessionSuccessfulAck] need to received
     * to mark that the content sync is completed
     */
    @Serializable
    @SerialName("ss_ak")
    data class SyncSessionSuccessfulAck(
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val reason: BLESyncCompletionReason = BLESyncCompletionReason.FULL_DUPLEX_SYNC_COMPLETED
    ) : BLESyncSession


    /**
     * Sent when the session must terminate due to an error.
     * @property reason The specific error that caused the failure.
     */
    @Serializable
    @SerialName("ssf")
    data class SyncSessionFailed(
        @ProtoNumber(1) override val sessionId: Uuid,
        @ProtoNumber(2) val reason: BLESyncFailedReason,
        @ProtoNumber(3) val isCritical: Boolean = false,
    ) : BLESyncSession

}
