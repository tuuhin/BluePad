package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.feature_sync.event.SyncChangesScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.ReviewSyncChangesScreenState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_basic_diff
import com.sam.bluepad.resources.ic_file_not_found
import org.jetbrains.compose.resources.painterResource

@Immutable
private sealed interface SyncChangesScreenModes {
    data object Loading : SyncChangesScreenModes
    data object SyncChangesList : SyncChangesScreenModes
    data object EmptySyncChanges : SyncChangesScreenModes
    data class Error(val errorText: String) : SyncChangesScreenModes
}


@Composable
fun SyncChangesItemsBoxWrapper(
    state: ReviewSyncChangesScreenState,
    onEvent: (SyncChangesScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {

    val screenMode by remember(state) {
        derivedStateOf {
            when {
                !state.isLoaded -> SyncChangesScreenModes.Loading
                state.errorMessage != null -> SyncChangesScreenModes.Error(state.errorMessage)
                state.changesList.isEmpty() -> SyncChangesScreenModes.EmptySyncChanges
                else -> SyncChangesScreenModes.SyncChangesList
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = screenMode,
            modifier = Modifier.animateContentSize(),
            contentAlignment = Alignment.Center,
        ) { mode ->
            when (mode) {
                SyncChangesScreenModes.SyncChangesList -> SyncChangesItemList(
                    items = state.changesList,
                    onEvent = onEvent,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxHeight(),
                )

                SyncChangesScreenModes.Loading -> SyncDiffsLoadingBox()
                SyncChangesScreenModes.EmptySyncChanges -> NoSyncChangesBox()
                is SyncChangesScreenModes.Error -> SyncDiffListErrorBox(error = mode.errorText)
            }
        }
    }
}

@Composable
private fun SyncDiffsLoadingBox(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        LoadingIndicator()
        Text(
            text = "Loading",
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SyncDiffListErrorBox(
    modifier: Modifier = Modifier,
    error: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.wrapContentSize(),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_file_not_found),
            contentDescription = "Missing sync file",
            modifier = Modifier.size(64.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.errorContainer),
        )
        Spacer(modifier = Modifier.heightIn(4.dp))
        Text(
            text = "Failed to read the sync changes",
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = error ?: "Error",
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun NoSyncChangesBox(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.wrapContentSize(),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_basic_diff),
            contentDescription = "Simple Diff icon",
            modifier = Modifier.size(64.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
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
}
