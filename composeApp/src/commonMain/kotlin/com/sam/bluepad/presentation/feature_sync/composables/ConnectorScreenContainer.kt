package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.feature_sync.state.ConnectorDiscoveryState
import com.sam.bluepad.presentation.feature_sync.state.ConnectorUIState
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

@Composable
fun ConnectorScreenContainer(
    discoveryState: ConnectorDiscoveryState,
    device: ExternalDeviceModel?,
    onStartConnector: () -> Unit,
    modifier: Modifier = Modifier,
    isAckReceived: Boolean = false,
    contentPadding: PaddingValues = PaddingValues.Zero
) {
    val connectorState by remember(discoveryState, device, isAckReceived) {
        derivedStateOf {
            when (discoveryState) {
                ConnectorDiscoveryState.DISCOVERING -> ConnectorUIState.Scanning
                ConnectorDiscoveryState.TIMEOUT -> ConnectorUIState.ScanTimeout
                ConnectorDiscoveryState.DISCOVERED if (device != null) ->
                    ConnectorUIState.DeviceDataRead(device)

                ConnectorDiscoveryState.DISCONNECTED if (device != null) ->
                    ConnectorUIState.DeviceDataRead(device)

                ConnectorDiscoveryState.DISCOVERED -> ConnectorUIState.ConnectedWithoutData
                ConnectorDiscoveryState.DISCONNECTED -> ConnectorUIState.DisconnectedWithoutData
                ConnectorDiscoveryState.NOT_STARTED -> ConnectorUIState.Idle
            }
        }
    }

    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SyncUIStateContainer(
            onStartConnector = onStartConnector,
            connectorState = connectorState,
        ) { device ->
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReceiverDeviceUICard(device = device)
            }
        }
    }
}

@Composable
private fun SyncUIStateContainer(
    connectorState: ConnectorUIState,
    onStartConnector: () -> Unit,
    modifier: Modifier = Modifier,
    onDeviceData: @Composable (ExternalDeviceModel) -> Unit,
) {
    val motionScheme = MaterialTheme.LocalMotionScheme.current

    AnimatedContent(
        targetState = connectorState,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            scaleIn(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeIn(animationSpec = motionScheme.slowEffectsSpec()) togetherWith
                    shrinkOut(animationSpec = motionScheme.defaultSpatialSpec()) +
                    fadeOut(animationSpec = motionScheme.slowEffectsSpec())
        }
    ) { state ->
        when (state) {
            ConnectorUIState.Scanning -> DeviceDiscoveryRunning()
            ConnectorUIState.ScanTimeout -> DeviceDiscoveryTimeout()
            ConnectorUIState.ConnectedWithoutData -> DeviceConnected()
            ConnectorUIState.DisconnectedWithoutData -> DeviceDisconnected()
            ConnectorUIState.Idle -> DeviceSyncStateIdle(onStart = onStartConnector)
            is ConnectorUIState.DeviceDataRead -> onDeviceData(state.device)
        }
    }
}

@Composable
private fun DeviceDiscoveryRunning(
    modifier: Modifier = Modifier,
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
        ContainedLoadingIndicator(
            modifier = Modifier.size(80.dp),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            indicatorColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.connector_discovery_running_title),
            style = titleStyle,
            color = titleColor
        )
        Text(
            text = stringResource(Res.string.connector_discovery_running_text),
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeviceSyncStateIdle(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    iconColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    Column(
        modifier = modifier.wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_sync_start),
            contentDescription = "Device timeout",
            modifier = Modifier.size(160.dp),
            colorFilter = ColorFilter.tint(iconColor)
        )
        Button(
            onClick = onStart,
            enabled = actionEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = ButtonDefaults.MediumContentPadding,
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.shape,
                pressedShape = ButtonDefaults.pressedShape
            )
        ) {
            Text(
                text = stringResource(Res.string.action_start_sync),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DeviceDiscoveryTimeout(
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.tertiary,
    titleStyle: TextStyle = MaterialTheme.typography.titleMediumEmphasized,
    textStyle: TextStyle = MaterialTheme.typography.bodySmallEmphasized,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier = modifier.wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_discovery_timeout),
            contentDescription = "Device timeout",
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(128.dp),
        )
        Text(
            text = stringResource(Res.string.connector_discovery_timeout_title),
            style = titleStyle,
            color = titleColor
        )
        Text(
            text = stringResource(Res.string.connector_discovery_timeout_text),
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeviceDisconnected(
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
            painter = painterResource(Res.drawable.ic_disconnected),
            contentDescription = "Device timeout",
            modifier = Modifier.size(128.dp),
            colorFilter = ColorFilter.tint(iconColor)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.connector_discovery_timeout_title),
            style = titleStyle,
            color = titleColor
        )
        Text(
            text = stringResource(Res.string.connector_discovery_timeout_text),
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
        )
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
            color = titleColor
        )
        Text(
            text = stringResource(Res.string.connector_discovery_connected_without_data_text),
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}