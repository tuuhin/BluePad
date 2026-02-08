package com.sam.bluepad.presentation.feature_sync

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
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
import com.sam.bluepad.presentation.feature_sync.composables.ConnectorScreenContainer
import com.sam.bluepad.presentation.feature_sync.composables.ConnectorScreenTopAppBar
import com.sam.bluepad.presentation.feature_sync.event.SyncConnectorScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.ConnectorDiscoveryState
import com.sam.bluepad.presentation.feature_sync.state.SyncConnectorScreenState
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
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
    ) { scPadding ->
        ConnectorScreenContainer(
            discoveryState = state.discoveryState,
            device = state.syncDevice,
            isAckReceived = state.isConnAckReceived,
            onStartConnector = { onEvent(SyncConnectorScreenEvent.StartClientConnection) },
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

private class SyncConnectorScreenStatePreviewParams :
    PreviewParameterProvider<SyncConnectorScreenState> {

    override val values: Sequence<SyncConnectorScreenState>
        get() = sequenceOf(
            SyncConnectorScreenState(),
            SyncConnectorScreenState(discoveryState = ConnectorDiscoveryState.NOT_STARTED),
            SyncConnectorScreenState(
                syncDevice = PreviewFakes.FAKE_EXTERNAL_MODEL,
                discoveryState = ConnectorDiscoveryState.DISCOVERED
            )
        )
}

@Preview
@Composable
private fun SyncConnectorScreenPreview(
    @PreviewParameter(SyncConnectorScreenStatePreviewParams::class)
    state: SyncConnectorScreenState,
) = BluePadTheme {
    SyncConnectorScreen(
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