package com.sam.bluepad.presentation.feature_sync

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverScreenTopAppbar
import com.sam.bluepad.presentation.feature_sync.composables.SyncReceiverScreenContent
import com.sam.bluepad.presentation.feature_sync.event.SyncReceiverScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncReceiverScreenState
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncReceiverScreen(
    state: SyncReceiverScreenState,
    onEvent: (SyncReceiverScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
) {
    val scrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackBarHostState = LocalSnackBarState.current

    val isSyncRunning by remember(state) {
        derivedStateOf { state.syncPhase.isSyncing }
    }

    Scaffold(
        topBar = {
            ReceiverScreenTopAppbar(
                scrollBehaviour = scrollBehaviour,
                isSyncRunning = isSyncRunning,
                navigation = navigation,
                onStopOrCancelSync = { onEvent(SyncReceiverScreenEvent.StopSyncReceiver) },
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
    ) { scPadding ->
        SyncReceiverScreenContent(
            screenState = state,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize().padding(
                horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING,
            ),
            contentPadding = scPadding,
        )
    }
}

private class SyncReceiverScreenStatePreviewParams :
    PreviewParameterProvider<SyncReceiverScreenState> {

    override val values: Sequence<SyncReceiverScreenState>
        get() = sequenceOf(
            SyncReceiverScreenState(),
            SyncReceiverScreenState(isReceiverRunning = true),
            SyncReceiverScreenState(
                currentDevice = PreviewFakes.FAKE_LOCAL_DEVICE_MODEL,
                foreignDevice = PreviewFakes.FAKE_EXTERNAL_MODEL_2,
                localDevicePlatformOS = DevicePlatformOS.ANDROID,
                isReceiverRunning = true,
                syncPhase = SyncUIState.HalfDuplexCompleted,
            ),
            SyncReceiverScreenState(
                currentDevice = PreviewFakes.FAKE_LOCAL_DEVICE_MODEL,
                foreignDevice = PreviewFakes.FAKE_EXTERNAL_MODEL_2,
                localDevicePlatformOS = DevicePlatformOS.ANDROID,
                isReceiverRunning = true,
                syncPhase = SyncUIState.FullSyncSuccessFull,
            ),
        )
}

@Preview
@Composable
private fun SyncReceiverScreenPreview(
    @PreviewParameter(SyncReceiverScreenStatePreviewParams::class)
    state: SyncReceiverScreenState
) = BluePadTheme {
    SyncReceiverScreen(
        state = state,
        onEvent = {},
        navigation = {
            Icon(
                painter = painterResource(Res.drawable.ic_back),
                contentDescription = stringResource(Res.string.action_back),
            )
        },
    )
}
