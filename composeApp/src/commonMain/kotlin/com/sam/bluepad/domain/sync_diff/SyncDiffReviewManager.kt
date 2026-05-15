package com.sam.bluepad.domain.sync_diff

import kotlin.uuid.Uuid

interface SyncDiffReviewManager {

    suspend fun readSyncSession(session: Uuid): Result<List<SyncChanges>>

    suspend fun submitSyncChanges(changes: List<SyncChanges>): Result<Unit>
}
