package com.sam.bluepad.domain.sync_diff

import kotlin.uuid.Uuid

sealed interface SyncChanges {

    val identity: Uuid

    data class Insert(val incoming: SyncSnapshotModel) : SyncChanges {

        override val identity: Uuid
            get() = incoming.id
    }

    data class Update(
        val local: SyncSnapshotModel,
        val incoming: SyncSnapshotModel,
    ) : SyncChanges {

        override val identity: Uuid
            get() = incoming.id

        val hasTitleChanged: Boolean
            get() = local.title != incoming.title

        val hasContentChanged: Boolean
            get() = local.contentHash != incoming.contentHash
    }

    data class Conflict(
        val local: SyncSnapshotModel,
        val incoming: SyncSnapshotModel,
    ) : SyncChanges {

        override val identity: Uuid
            get() = incoming.id

        val hasTitleChanged: Boolean
            get() = local.title != incoming.title

        val hasContentChanged: Boolean
            get() = local.contentHash != incoming.contentHash

        val isVersionSame: Boolean
            get() = local.version == incoming.version

        val isModifiedAtSame: Boolean
            get() = local.modifiedAt == incoming.modifiedAt
    }

    data class Delete(
        val local: SyncSnapshotModel,
        val incoming: SyncSnapshotModel,
    ) : SyncChanges {

        override val identity: Uuid
            get() = incoming.id
    }
}
