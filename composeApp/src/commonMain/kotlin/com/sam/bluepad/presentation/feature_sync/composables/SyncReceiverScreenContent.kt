package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.presentation.feature_sync.event.SyncReceiverScreenEvent
import com.sam.bluepad.presentation.feature_sync.state.SyncReceiverScreenState
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_antenna
import com.sam.bluepad.resources.ic_sync_failed
import com.sam.bluepad.resources.receiver_sync_devices_empty_text
import com.sam.bluepad.resources.receiver_sync_devices_empty_title
import com.sam.bluepad.resources.receiver_sync_devices_receiver_running_text
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private sealed class ScreenState {

    data class ScannerNotStarted(val isChecked: Boolean = false) : ScreenState()
    data object ScanRunning : ScreenState()

    data class ContentSyncing(
        val device: ExternalDeviceModel,
        val currentDevice: LocalDeviceInfoModel,
        val localDevicePlatformOS: DevicePlatformOS
    ) : ScreenState()

    data class SyncFailed(val message: String) : ScreenState()
}

@Composable
fun SyncReceiverScreenContent(
    screenState: SyncReceiverScreenState,
    onEvent: (SyncReceiverScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero
) {

    val motionScheme = LocalMaterialTheme.current.motionScheme

    val state by remember(screenState) {
        derivedStateOf {
            when {
                screenState.isReceiverRunning && screenState.foreignDevice != null && screenState.currentDevice != null ->
                    ScreenState.ContentSyncing(
                        screenState.foreignDevice,
                        screenState.currentDevice,
                        screenState.localDevicePlatformOS,
                    )

                screenState.isReceiverRunning -> ScreenState.ScanRunning
                screenState.syncPhase is SyncUIState.Failed -> ScreenState.SyncFailed(screenState.syncPhase.message)
                else -> ScreenState.ScannerNotStarted(isChecked = screenState.advertisedAtLeastOnce)
            }
        }
    }

    AnimatedContent(
        targetState = state,
        modifier = modifier.padding(contentPadding),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            scaleIn(animationSpec = motionScheme.defaultSpatialSpec()) +
                fadeIn(animationSpec = motionScheme.slowEffectsSpec()) togetherWith
                shrinkOut(animationSpec = motionScheme.defaultSpatialSpec()) +
                fadeOut(animationSpec = motionScheme.slowEffectsSpec())
        },
    ) { uiState ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is ScreenState.ContentSyncing -> ReceiverSyncingDataContainer(
                    externalDevice = uiState.device,
                    currentDevice = uiState.currentDevice,
                    currentDevicePlatform = uiState.localDevicePlatformOS,
                    syncState = screenState.syncPhase,
                    contentPadding = PaddingValues(16.dp),
                    onCheckSketches = { onEvent(SyncReceiverScreenEvent.NavigateToSketches) },
                    modifier = Modifier.fillMaxSize(),
                )

                ScreenState.ScanRunning -> ReceiverRunningContainer(
                    onStop = { onEvent(SyncReceiverScreenEvent.StopSyncReceiver) },
                )

                is ScreenState.ScannerNotStarted -> StartReceiverContainer(
                    isRetry = uiState.isChecked,
                    onStart = { onEvent(SyncReceiverScreenEvent.StartSyncReceiver) },
                )

                is ScreenState.SyncFailed -> ReceiverSyncFailedContent(
                    message = uiState.message,
                    onDisconnect = { onEvent(SyncReceiverScreenEvent.DisconnectAndReset) },
                )
            }
        }
    }
}

@Composable
private fun ReceiverSyncFailedContent(
    message: String,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_sync_failed),
            contentDescription = "Sync failed",
            modifier = Modifier.size(200.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Unable to continue with sync",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDisconnect,
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
                text = "Disconnect",
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StartReceiverContainer(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    isRetry: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_antenna),
            contentDescription = "Receiver Antenna",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start Receiver",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Receiver will scan for saved devices in close proximity and start sync if found",
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 220.dp),
            textAlign = TextAlign.Center,
        )
        AnimatedVisibility(isRetry) {
            Text(
                text = stringResource(Res.string.receiver_sync_devices_receiver_running_text),
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.heightIn(ButtonDefaults.ExtraSmallContainerHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.elevatedShape,
                pressedShape = ButtonDefaults.mediumPressedShape,
            ),
        ) {
            Text(
                text = "Start Receiver",
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ReceiverRunningContainer(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingIndicator(modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.receiver_sync_devices_empty_title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.receiver_sync_devices_empty_text),
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onStop,
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
