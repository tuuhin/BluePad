package com.sam.bluepad.presentation.feature_sync

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.sam.bluepad.presentation.feature_sync.composables.ConnectorScreenContainer
import com.sam.bluepad.presentation.feature_sync.composables.ConnectorScreenTopAppBar
import com.sam.bluepad.presentation.feature_sync.event.SyncConnectorScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncConnectorScreenState
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncConnectorScreen(
    state: SyncConnectorScreenState,
    onEvent: (SyncConnectorScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
) {

    val scrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackBarHostState = LocalSnackBarState.current
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        topBar = {
            ConnectorScreenTopAppBar(
                scrollBehaviour = scrollBehaviour,
                isConnectorRunning = state.isConnectorRunning,
                isReadyToSync = state.isReadyToSync,
                navigation = navigation,
                onStopConnection = { onEvent(SyncConnectorScreenEvent.StopClientConnection) },
                onStartConnection = { onEvent(SyncConnectorScreenEvent.StartClientConnection) },
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
    ) { scPadding ->
        ConnectorScreenContainer(
            discoveryState = state.deviceDiscoveryState,
            device = state.syncDevice,
            isAckReceived = state.isConnAckReceived,
            contentPadding = PaddingValues(
                start = scPadding.calculateStartPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                end = scPadding.calculateEndPadding(layoutDirection) + Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                top = scPadding.calculateTopPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING,
                bottom = scPadding.calculateBottomPadding() + Dimensions.SCAFFOLD_VERTICAL_PADDING
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@PreviewLightDark
@Composable
private fun SyncConnectorScreenPreview() = BluePadTheme {
    SyncConnectorScreen(
        state = SyncConnectorScreenState(),
        onEvent = {},
    )
}