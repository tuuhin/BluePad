package com.sam.bluepad.presentation.feature_sync

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.feature_sync.composables.SyncChangesItemList
import com.sam.bluepad.presentation.feature_sync.event.SyncChangesScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncDiffListUIState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_basic_diff
import com.sam.bluepad.resources.ic_file_not_found
import com.sam.bluepad.resources.ic_receiver_action_reset
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource

@Composable
fun SyncChangesSheetContent(
    state: SyncDiffListUIState,
    onEvent: (SyncChangesScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {

    val screenMode by remember(state) {
        derivedStateOf {
            when {
                state.isLoaded -> SyncChangesScreenModes.Loading
                state.isError && state.errorMessage != null -> SyncChangesScreenModes.Error(state.errorMessage)
                state.syncList.isEmpty() -> SyncChangesScreenModes.EmptySyncChanges
                else -> SyncChangesScreenModes.SyncChangesList
            }
        }
    }

    Column(
        modifier = modifier.padding(
            horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
            vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Review your changes",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider()
        SyncChangesContentWrapper(
            screenMode = screenMode,
            modifier = Modifier.fillMaxWidth().heightIn(200.dp),
        ) {
            SyncChangesItemList(items = state.syncList)
        }
        // action list
        HorizontalDivider()
        SubmitSyncChangesActionList(
            onSaveSelectedChanges = { onEvent(SyncChangesScreenEvent.OnApproveAction) },
            onCancel = { onEvent(SyncChangesScreenEvent.OnCancelAction) },
        )
    }
}

@Composable
private fun SubmitSyncChangesActionList(
    onSaveSelectedChanges: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier.minimumInteractiveComponentSize()
                .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = IconButtonDefaults.largeRoundShape,
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_receiver_action_reset),
                contentDescription = "Reset Receiver",
            )
        }

        Button(
            onClick = onSaveSelectedChanges,
            modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.elevatedShape,
                pressedShape = ButtonDefaults.mediumPressedShape,
            ),
            enabled = enabled,
        ) {
            Text(
                text = "Save",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }
    }
}

@Composable
private fun SyncChangesContentWrapper(
    screenMode: SyncChangesScreenModes,
    modifier: Modifier = Modifier,
    listContent: @Composable () -> Unit,
) {
    AnimatedContent(
        targetState = screenMode,
        modifier = modifier.animateContentSize(),
        contentAlignment = Alignment.Center,
    ) { mode ->
        when (mode) {
            SyncChangesScreenModes.SyncChangesList -> listContent()
            SyncChangesScreenModes.Loading -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LoadingIndicator()
                Text(
                    text = "Loading",
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SyncChangesScreenModes.EmptySyncChanges -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(
                    painter = painterResource(Res.drawable.ic_basic_diff),
                    contentDescription = "Simple Diff icon",
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.heightIn(4.dp))
                Text(
                    text = "No changes found",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Content on both of the device is same",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            is SyncChangesScreenModes.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(
                        painter = painterResource(Res.drawable.ic_file_not_found),
                        contentDescription = "Missing sync file",
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.heightIn(4.dp))
                    Text(
                        text = "Failed to read the sync changes",
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = mode.errorText,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Immutable
private sealed interface SyncChangesScreenModes {
    data object Loading : SyncChangesScreenModes
    data object SyncChangesList : SyncChangesScreenModes
    data object EmptySyncChanges : SyncChangesScreenModes
    data class Error(val errorText: String) : SyncChangesScreenModes
}
