package com.sam.bluepad.domain.sync_diff

import com.sam.bluepad.domain.sync.models.SyncContentDataModel

fun interface SyncDiffCalculator {

    suspend fun computeDiff(changes: List<SyncContentDataModel>): Result<Set<SyncChanges>>
}
