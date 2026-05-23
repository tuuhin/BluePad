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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.presentation.feature_sync.event.SyncChangesScreenEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format

@Composable
fun SyncChangesItemList(
    items: ImmutableList<SyncChanges>,
    onEvent: (SyncChangesScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val isInspectionMode = LocalInspectionMode.current

    val keys: ((Int, SyncChanges) -> Any)? = remember {
        if (isInspectionMode) return@remember null
        { _, state -> state.identity.toHexString() }
    }

    val contentType: (Int, SyncChanges) -> Any? = remember {
        { _, state -> state.javaClass.simpleName }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(
            items, key = keys,
            contentType = contentType,
        ) { _, state ->
            when (state) {
                is SyncChanges.Conflict -> SyncChangeConflictCard(
                    change = state,
                    onKeepIncoming = { onEvent(SyncChangesScreenEvent.OnResolveConflict(state.identity, true)) },
                    onKeepLocal = { onEvent(SyncChangesScreenEvent.OnResolveConflict(state.identity, false)) },
                )

                is SyncChanges.Delete -> SyncChangeDeleteCard(change = state)
                is SyncChanges.Insert -> SyncChangeInsertCard(change = state)
                is SyncChanges.Update -> SyncChangeUpdateCard(change = state)
            }
        }
    }
}

@Composable
private fun SyncChangeInsertCard(
    change: SyncChanges.Insert,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val extraEntries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "Content Hash" to change.incoming.contentHash,
                "Created at" to change.incoming.modifiedAt.format(LocalDateTime.Formats.ISO),
            )
        }
    }

    val entries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "Title" to change.incoming.title,
                "Content" to change.incoming.content,
            )
        }
    }

    SyncChangeBaseCard(
        modifier = modifier,
        shape = shape,
        change = change,
        contentPadding = contentPadding,
        extraEntries = extraEntries,
        entries = entries,
    )
}

@Composable
private fun SyncChangeUpdateCard(
    change: SyncChanges.Update,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val entries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "New Title" to change.incoming.title,
                "New Content" to change.incoming.content,
                "Status" to buildString {
                    if (change.hasTitleChanged) append("Title Updated. ")
                    if (change.hasContentChanged) append("Content Updated.")
                    if (!change.hasTitleChanged && !change.hasContentChanged) append("No visible changes.")
                },
            )
        }
    }

    val extraEntries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "Old Title" to change.local.title,
                "Old Content" to change.local.content,
                "Old Version" to change.local.version.toString(),
                "New Version" to change.incoming.version.toString(),
            )
        }
    }

    SyncChangeBaseCard(
        modifier = modifier,
        shape = shape,
        change = change,
        contentPadding = contentPadding,
        extraEntries = extraEntries,
        entries = entries,
    )
}

@Composable
private fun SyncChangeDeleteCard(
    change: SyncChanges.Delete,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val hasTitleChanged = change.local.title != change.incoming.title
    val hasContentChanged = change.local.contentHash != change.incoming.contentHash

    val entries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "Title" to change.incoming.title,
                "Content" to change.incoming.content,
                "Status" to buildString {
                    append("To be deleted. ")
                    if (hasTitleChanged) append("Title updated. ")
                    if (hasContentChanged) append("Content updated.")
                },
            )
        }
    }

    val extraEntries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "Local Title" to change.local.title,
                "Local Content" to change.local.content,
                "Deleted at" to change.incoming.modifiedAt.format(LocalDateTime.Formats.ISO),
            )
        }
    }

    SyncChangeBaseCard(
        modifier = modifier,
        shape = shape,
        change = change,
        contentPadding = contentPadding,
        extraEntries = extraEntries,
        entries = entries,
    )
}

@Composable
private fun SyncChangeConflictCard(
    change: SyncChanges.Conflict,
    onKeepIncoming: () -> Unit,
    onKeepLocal: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val entries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "Incoming Title" to change.incoming.title,
                "Incoming Content" to change.incoming.content,
                "Status" to buildString {
                    append("CONFLICT! ")
                    if (!change.isVersionSame) append("Version mismatch. ")
                    if (!change.isModifiedAtSame) append("Both edited. ")
                    if (change.hasTitleChanged) append("Title Conflict. ")
                    if (change.hasContentChanged) append("Content Conflict.")
                },
            )
        }
    }

    val extraEntries by remember(change) {
        derivedStateOf {
            persistentMapOf(
                "Local Title" to change.local.title,
                "Local Content" to change.local.content,
                "Local Version" to change.local.version.toString(),
                "Incoming Version" to change.incoming.version.toString(),
            )
        }
    }

    SyncChangeBaseCard(
        modifier = modifier,
        shape = shape,
        change = change,
        contentPadding = contentPadding,
        extraEntries = extraEntries,
        entries = entries,
        actions = {
            Row(
                modifier = Modifier.align(Alignment.End),
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
        },
    )
}
