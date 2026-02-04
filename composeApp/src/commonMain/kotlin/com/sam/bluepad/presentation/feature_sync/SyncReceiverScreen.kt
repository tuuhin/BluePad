package com.sam.bluepad.presentation.feature_sync

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import com.sam.bluepad.presentation.composables.ContentLoadingWrapper
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverFoundContainer
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverRunningOrNoPeerContainer
import com.sam.bluepad.presentation.feature_sync.composables.ReceiverScreenTopAppbar
import com.sam.bluepad.presentation.feature_sync.event.SyncReceiverScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncReceiverScreenState
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.theme.Dimensions

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
