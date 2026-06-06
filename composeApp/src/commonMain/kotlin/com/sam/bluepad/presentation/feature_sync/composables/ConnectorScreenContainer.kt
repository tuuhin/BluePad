package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.presentation.feature_sync.state.DiscoveryUIState
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_start_sync
import com.sam.bluepad.resources.connector_discovery_connected_without_data_text
import com.sam.bluepad.resources.connector_discovery_connected_without_data_title
import com.sam.bluepad.resources.connector_discovery_running_text
import com.sam.bluepad.resources.connector_discovery_running_title
import com.sam.bluepad.resources.connector_discovery_timeout_text
import com.sam.bluepad.resources.connector_discovery_timeout_title
import com.sam.bluepad.resources.ic_connection_cable
import com.sam.bluepad.resources.ic_disconnected
import com.sam.bluepad.resources.ic_discovery_timeout
import com.sam.bluepad.resources.ic_sync_start
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Stable
private sealed interface ConnectorScreenState {

    // discovery
    data object Idle : ConnectorScreenState
    data object Scanning : ConnectorScreenState
    data object ScanTimeout : ConnectorScreenState
    data object DeviceDisconnected : ConnectorScreenState

    // handshake connection
    data object ProcessingInitialConnection : ConnectorScreenState

    // sync phase
    data class SyncPhase(val device: ExternalDeviceModel) : ConnectorScreenState
}

@Composable
fun ConnectorScreenContainer(
    discoveryState: DiscoveryUIState,
    localDevice: LocalDeviceInfoModel?,
    onStartConnector: () -> Unit,
    onStopScan: () -> Unit,
    onRetryConnection: () -> Unit,
    onReviewSketches: () -> Unit,
    onDisconnect:()-> Unit,
    modifier: Modifier = Modifier,
    syncState: SyncUIState = SyncUIState.NotRunning,
    devicePlatformOS: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    contentPadding: PaddingValues = PaddingValues.Zero
) {

    val motionScheme = LocalMaterialTheme.current.motionScheme

    val connectorState by remember(discoveryState) {
        derivedStateOf {
            when (discoveryState) {
                DiscoveryUIState.Discovering -> ConnectorScreenState.Scanning
                DiscoveryUIState.Timeout -> ConnectorScreenState.ScanTimeout
                DiscoveryUIState.Discovered -> ConnectorScreenState.ProcessingInitialConnection
                DiscoveryUIState.Disconnected -> ConnectorScreenState.DeviceDisconnected
                is DiscoveryUIState.DeviceConnected -> ConnectorScreenState.SyncPhase(discoveryState.device)
                else -> ConnectorScreenState.Idle
            }
        }
    }

    Box(
        modifier = modifier.padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = connectorState,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                scaleIn(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motionScheme.slowEffectsSpec()) togetherWith
                    shrinkOut(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeOut(animationSpec = motionScheme.slowEffectsSpec())
            },
        ) { state ->
            when (state) {
                ConnectorScreenState.Idle -> DeviceSyncStateIdle(onStart = onStartConnector)
                ConnectorScreenState.Scanning -> DeviceDiscoveryRunning(onStopScan = onStopScan)
                ConnectorScreenState.ScanTimeout, ConnectorScreenState.DeviceDisconnected -> DeviceDisconnectedOrTimeout(onRetry = onRetryConnection)
                ConnectorScreenState.ProcessingInitialConnection -> DeviceConnected()
                is ConnectorScreenState.SyncPhase if (localDevice != null) -> SyncingRunningDataContainer(
                    externalDevice = state.device,
                    currentDevice = localDevice,
                    currentDevicePlatform = devicePlatformOS,
                    syncState = syncState,
                    isLocalDeviceReceiver = false,
                    contentPadding = PaddingValues(16.dp),
                    onCheckSketches = onReviewSketches,
                    onDisconnectAndReset = onDisconnect,
                )

                else -> {}
            }
        }
    }
}


@Composable
private fun DeviceDiscoveryRunning(
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMediumEmphasized,
    textStyle: TextStyle = MaterialTheme.typography.bodyMediumEmphasized,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContainedLoadingIndicator(
            modifier = Modifier.size(80.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.connector_discovery_running_title),
            style = titleStyle,
            color = titleColor,
        )
        Text(
            text = stringResource(Res.string.connector_discovery_running_text),
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onStopScan,
            modifier = Modifier.heightIn(ButtonDefaults.ExtraSmallContainerHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.elevatedShape,
                pressedShape = ButtonDefaults.mediumPressedShape,
            ),
        ) {
            Text(
                text = "Stop Receiver",
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DeviceSyncStateIdle(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    iconColor: Color = MaterialTheme.colorScheme.tertiary,
    titleStyle: TextStyle = MaterialTheme.typography.titleMediumEmphasized,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = MaterialTheme.typography.bodyMediumEmphasized,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_sync_start),
            contentDescription = "Start device sync",
            modifier = Modifier.size(160.dp),
            colorFilter = ColorFilter.tint(iconColor),
        )
        Text(
            text = "Start Sync",
            style = titleStyle,
            color = titleColor,
        )
        Text(
            text = "Will look for your external device in close proximity and start sync when done",
            style = textStyle,
            color = textColor,
            modifier = Modifier.widthIn(min = 220.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onStart,
            enabled = actionEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            contentPadding = ButtonDefaults.MediumContentPadding,
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.shape,
                pressedShape = ButtonDefaults.pressedShape,
            ),
        ) {
            Text(
                text = stringResource(Res.string.action_start_sync),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DeviceDisconnectedOrTimeout(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isTimeout: Boolean = false,
    iconColor: Color = MaterialTheme.colorScheme.tertiary,
    titleStyle: TextStyle = MaterialTheme.typography.titleMediumEmphasized,
    textStyle: TextStyle = MaterialTheme.typography.bodySmallEmphasized,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isTimeout) {
            Image(
                painter = painterResource(Res.drawable.ic_discovery_timeout),
                contentDescription = "Device timeout",
                colorFilter = ColorFilter.tint(iconColor),
                modifier = Modifier.size(128.dp),
            )
            Text(
                text = stringResource(Res.string.connector_discovery_timeout_title),
                style = titleStyle,
                color = titleColor,
            )
            Text(
                text = stringResource(Res.string.connector_discovery_timeout_text),
                style = textStyle,
                color = textColor,
                modifier = Modifier.widthIn(min = 220.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.ic_disconnected),
                contentDescription = "Device timeout",
                modifier = Modifier.size(128.dp),
                colorFilter = ColorFilter.tint(iconColor),
            )
            Text(
                text = stringResource(Res.string.connector_discovery_timeout_title),
                style = titleStyle,
                color = titleColor,
            )
            Text(
                text = stringResource(Res.string.connector_discovery_timeout_text),
                style = textStyle,
                color = textColor,
                modifier = Modifier.widthIn(min = 220.dp),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.heightIn(ButtonDefaults.ExtraSmallContainerHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.elevatedShape,
                pressedShape = ButtonDefaults.mediumPressedShape,
            ),
        ) {
            Text(
                text = "Retry",
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}


@Composable
private fun DeviceConnected(
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.tertiary,
    titleStyle: TextStyle = MaterialTheme.typography.titleMediumEmphasized,
    textStyle: TextStyle = MaterialTheme.typography.bodyMediumEmphasized,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier.wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_connection_cable),
            contentDescription = "Device connected",
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(128.dp),
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = stringResource(Res.string.connector_discovery_connected_without_data_title),
            style = titleStyle,
            color = titleColor,
        )
        Text(
            text = stringResource(Res.string.connector_discovery_connected_without_data_text),
            style = textStyle,
            color = textColor,
            modifier = Modifier.widthIn(min = 220.dp),
            textAlign = TextAlign.Center,
        )
    }
}
