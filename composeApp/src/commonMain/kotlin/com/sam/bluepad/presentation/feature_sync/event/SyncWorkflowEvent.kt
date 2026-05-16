package com.sam.bluepad.presentation.feature_sync.event

import kotlin.uuid.Uuid

sealed interface SyncWorkflowEvent {

    data class ReadyForReview(val sessionId: Uuid) : SyncWorkflowEvent
}
