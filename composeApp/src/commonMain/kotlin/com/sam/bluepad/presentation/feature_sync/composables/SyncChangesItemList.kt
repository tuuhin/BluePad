package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.presentation.feature_sync.event.SyncChangesScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.ApprovedSyncChanges
import com.sam.bluepad.presentation.feature_sync.state.ConflictResolutionState
import com.sam.bluepad.presentation.utils.formatToTimeStamp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun SyncChangesItemList(
    items: ImmutableList<ApprovedSyncChanges>,
    onEvent: (SyncChangesScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val isInspectionMode = LocalInspectionMode.current

    val keys: ((Int, ApprovedSyncChanges) -> Any)? = remember {
        if (isInspectionMode) return@remember null
        { _, state -> state.change.identity.toHexString() }
    }

    val contentType: (Int, ApprovedSyncChanges) -> Any? = remember {
        { _, state -> state.change.javaClass.simpleName }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(
            items, key = keys,
            contentType = contentType,
        ) { _, syncChange ->
            SyncChangeBaseCard(
                change = syncChange.change,
                shape = MaterialTheme.shapes.extraLarge,
                extraEntries = syncChange.change.extraMetaData,
                entries = syncChange.change.coreData,
                actions = {
                    when (syncChange.change) {
                        is SyncChanges.Conflict -> ConflictResolutionButtons(
                            onKeepLocal = {
                                onEvent(
                                    SyncChangesScreenEvent.OnResolveConflict(
                                        syncChange.change.identity,
                                        ConflictResolutionState.KEEP_LOCAL,
                                    ),
                                )
                            },
                            onKeepIncoming = {
                                onEvent(
                                    SyncChangesScreenEvent.OnResolveConflict(
                                        syncChange.change.identity,
                                        ConflictResolutionState.KEEP_REMOTE,
                                    ),
                                )
                            },
                        )

                        else -> {}
                    }
                },
            )
        }
    }
}


@Composable
private fun ConflictResolutionButtons(
    modifier: Modifier = Modifier,
    onKeepLocal: () -> Unit,
    onKeepIncoming: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onKeepLocal,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer),
        ) {
            Text("Keep Old")
        }
        Button(
            onClick = onKeepIncoming,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text("Keep Incoming")
        }
    }
}


private val SyncChanges.coreData: ImmutableMap<String, String>
    @Composable
    get() {
        val metadata = persistentMapOf<String, String>()
            .builder()
        when (this) {
            is SyncChanges.Insert -> {
                metadata["TITTLE"] = incoming.title
                metadata["CONTENT"] = incoming.content
            }

            is SyncChanges.Conflict -> {
                metadata["TITTLE"] = incoming.title
                metadata["CONTENT"] = incoming.content
            }

            is SyncChanges.Delete -> {
                metadata["TITTLE"] = incoming.title
                metadata["CONTENT"] = incoming.content
            }

            is SyncChanges.Update -> {
                metadata["TITTLE"] = incoming.title
                metadata["CONTENT"] = incoming.content
            }
        }
        return metadata.build()
    }

private val SyncChanges.extraMetaData: ImmutableMap<String, String>
    @Composable
    get() {
        val metadata = persistentMapOf<String, String>()
            .builder()

        metadata["_ID"] = identity.toHexString()
        when (this) {
            is SyncChanges.Insert -> {
                metadata["CONTENT HASH"] = incoming.contentHash
                metadata["CREATED ON"] = incoming.modifiedAt.formatToTimeStamp()
                metadata["VERSION"] = incoming.version.toString()
            }

            is SyncChanges.Delete -> {
                metadata["CONTENT HASH"] = incoming.contentHash
                metadata["MODIFIED ON"] = incoming.modifiedAt.formatToTimeStamp()
                metadata["VERSION"] = incoming.version.toString()
                metadata["IS CONTENT UPDATED"] =
                    if (incoming.contentHash != local.contentHash || incoming.title != local.title) "TRUE" else "FALSE"
            }

            is SyncChanges.Update -> {
                metadata["CONTENT HASH"] = incoming.contentHash
                metadata["MODIFIED ON"] = incoming.modifiedAt.formatToTimeStamp()
                metadata["OLD VERSION"] = local.version.toString()
                metadata["NEW VERSION"] = incoming.version.toString()
                metadata["IS TITLE UPDATED"] = if (incoming.title != local.title) "TRUE" else "FALSE"
            }

            is SyncChanges.Conflict -> {
                metadata["OLD CONTENT HASH"] = local.contentHash
                metadata["NEW CONTENT HASH"] = incoming.contentHash
                metadata["OLD MODIFIED AT"] = local.modifiedAt.formatToTimeStamp()
                metadata["NEW MODIFIED AT"] = incoming.modifiedAt.formatToTimeStamp()
                metadata["OLD VERSION"] = local.version.toString()
                metadata["NEW VERSION"] = incoming.version.toString()
                metadata["CONFLICT REASON"] = buildString {
                    if (!isVersionSame) append("VERSION CONFLICT")
                    if (!isModifiedAtSame) append("BOTH EDITED CONFLICT")
                    if (hasTitleChanged) append("TITLE CONFLICT")
                    if (hasContentChanged) append("CONTENT CONFLICT")
                }
            }
        }
        return metadata.build()
    }
