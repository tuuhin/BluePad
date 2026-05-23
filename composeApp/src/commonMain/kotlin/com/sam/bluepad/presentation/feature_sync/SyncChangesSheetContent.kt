package com.sam.bluepad.presentation.feature_sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.sync_diff.SyncChanges
import com.sam.bluepad.presentation.feature_sync.composables.SyncChangesItemsBoxWrapper
import com.sam.bluepad.presentation.feature_sync.event.SyncChangesScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.ReviewSyncChangesScreenState
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_cancel
import com.sam.bluepad.resources.ic_double_tick
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource

@Composable
fun ReviewSyncChangesSheetContent(
    state: ReviewSyncChangesScreenState,
    onEvent: (SyncChangesScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {

    val isSaveEnabled by remember(state) {
        derivedStateOf { state.syncList.isNotEmpty() }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Review your changes",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider()
        SyncChangesItemsBoxWrapper(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 240.dp)
                .weight(1f, fill = isSaveEnabled),
        )
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(
            visible = state.isLoaded,
            enter = expandVertically(),
            exit = shrinkVertically(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Its a one time sync changes preview exiting the screen will invalidate the changes and need to sync once again to preview or save",
                style = MaterialTheme.typography.labelSmallEmphasized,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        SubmitOrChangesActions(
            onSaveSelectedChanges = { onEvent(SyncChangesScreenEvent.OnApproveAction) },
            onCancel = { onEvent(SyncChangesScreenEvent.OnCancelAction) },
            isSaveEnabled = isSaveEnabled,
            isCancelEnabled = state.isLoaded,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun SubmitOrChangesActions(
    onSaveSelectedChanges: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isCancelEnabled: Boolean = true,
    isSaveEnabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier.heightIn(ButtonDefaults.ExtraSmallContainerHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = IconButtonDefaults.largeRoundShape,
            enabled = isCancelEnabled,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_cancel),
                contentDescription = "Reset Receiver",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }

        Button(
            onClick = onSaveSelectedChanges,
            modifier = Modifier.heightIn(ButtonDefaults.ExtraSmallContainerHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.elevatedShape,
                pressedShape = ButtonDefaults.mediumPressedShape,
            ),
            enabled = isSaveEnabled,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_double_tick),
                contentDescription = "Reset Receiver",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                text = "Save Changes",
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }
    }
}

private class SyncChangesUIStatePreviewParams : PreviewParameterProvider<ReviewSyncChangesScreenState> {
    override val values: Sequence<ReviewSyncChangesScreenState>
        get() = sequenceOf(
            ReviewSyncChangesScreenState(isLoaded = false),
            ReviewSyncChangesScreenState(isLoaded = true, isError = true, errorMessage = "Failed to load data"),
            ReviewSyncChangesScreenState(isLoaded = true, isError = false),
            ReviewSyncChangesScreenState(
                isLoaded = true, isError = false,
                syncList = persistentListOf<SyncChanges>()
                    .addAll(
                        listOf(
                            PreviewFakes.FAKE_SYNC_CHANGE_DELETE,
                            PreviewFakes.FAKE_SYNC_CHANGE_INSERT,
                            PreviewFakes.FAKE_SYNC_CHANGE_DELETE_WITH_UPDATED_CONTENT,
                            PreviewFakes.FAKE_SYNC_CHANGE_UPDATE,
                        ),
                    ),
            ),
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ReviewSyncChangesSheetContentPreview(
    @PreviewParameter(SyncChangesUIStatePreviewParams::class)
    state: ReviewSyncChangesScreenState
) = BluePadTheme {
    Surface(
        color = BottomSheetDefaults.ContainerColor,
        shape = BottomSheetDefaults.ExpandedShape,
    ) {
        ReviewSyncChangesSheetContent(
            state = state,
            onEvent = {},
            modifier = Modifier.padding(all = Dimensions.MODAL_BOTTOM_SHEET_CONTENT_PADDING),
        )
    }
}
