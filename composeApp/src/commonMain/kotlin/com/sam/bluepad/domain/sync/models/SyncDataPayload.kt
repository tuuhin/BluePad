package com.sam.bluepad.domain.sync.models

import kotlin.uuid.Uuid

sealed interface SyncDataPayload {

    /**
     * Payloads that can be prepared and sent to a peer.
     */
    sealed interface Outgoing : SyncDataPayload

    /**
     * Results produced after processing incoming data from a peer,
     * often indicating the next step in the sync flow.
     */
    sealed interface ProcessedResult : SyncDataPayload

    /**
     * Initial sync state: sends a list of all local item metadata to the peer.
     */
    data object Metadata : Outgoing

    /**
     * Request for specific content after comparing metadata.
     */
    data class ContentQuery(val ids: List<Uuid>) : Outgoing, ProcessedResult

    /**
     * The actual content data being transferred.
     */
    data class ContentPayload(val ids: List<Uuid>) : Outgoing, ProcessedResult

    /**
     * Indicates that the incoming data was processed and no further outgoing action is required.
     */
    data object NoAction : ProcessedResult
}