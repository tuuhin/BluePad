package com.sam.bluepad.presentation.feature_sync

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.sam.bluepad.presentation.composables.ContentLoadingWrapper
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverFoundContainer
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverRunningOrNoPeerContainer
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverScreenTopAppbar
import com.sam.bluepad.presentation.feature_sync.event.SyncReceiverScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncReceiverScreenState
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
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        topBar = {
            ReceiverScreenTopAppbar(
                scrollBehaviour = scrollBehaviour,
                isAdvertising = state.isReceiverRunning,
                navigation = navigation,
                onStartAdvertising = { onEvent(SyncReceiverScreenEvent.StartSyncReceiver) },
                onStopAdvertising = { onEvent(SyncReceiverScreenEvent.StopSyncReceiver) }
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
    ) { scPadding ->
        ContentLoadingWrapper(
            content = state.foreignDevice,
            isLoading = false,
            modifier = Modifier.padding(
                start = scPadding.calculateStartPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                end = scPadding.calculateEndPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                top = scPadding.calculateTopPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING,
                bottom = scPadding.calculateBottomPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING,
            ),
            onSuccess = { foreignDevice ->
                ReceiverFoundContainer(
                    externalDevice = foreignDevice,
                    currentDevice = state.currentDevice,
                    onStartSync = { onEvent(SyncReceiverScreenEvent.OnStartSyncConnection) },
                    onRejectDevice = { onEvent(SyncReceiverScreenEvent.OnRejectSyncConnection) },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            onFailed = {
                ReceiverRunningOrNoPeerContainer(
                    isRunning = state.isReceiverRunning,
                    modifier = Modifier.fillMaxSize()
                )
            },
        )
    }
}

private class SyncReceiverScreenStatePreviewParams :
    PreviewParameterProvider<SyncReceiverScreenState> {

    override val values: Sequence<SyncReceiverScreenState>
        get() = sequenceOf(
            SyncReceiverScreenState(),
            SyncReceiverScreenState(isReceiverRunning = true),
            SyncReceiverScreenState(isSyncRunning = true),
            SyncReceiverScreenState(
                currentDevice = PreviewFakes.FAKE_EXTERNAL_MODEL,
                foreignDevice = PreviewFakes.FAKE_EXTERNAL_MODEL_2,
                isReceiverRunning = false
            )
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
                contentDescription = stringResource(Res.string.action_back)
            )
        }
    )
}
