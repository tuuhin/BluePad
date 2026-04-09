package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState

@Composable
fun ReceiverSyncingDataContainer(
    externalDevice: ExternalDeviceModel,
    currentDevice: LocalDeviceInfoModel,
    onCheckSketches: () -> Unit,
    modifier: Modifier = Modifier,
    currentDevicePlatform: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    syncState: SyncUIState = SyncUIState.NotRunning,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val windowInfo = currentWindowAdaptiveInfo()

    val receiverCardScale by animateFloatAsState(
        when (syncState) {
            is SyncUIState.Running -> 1.1f
            is SyncUIState.HalfDuplex -> .8f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
    )

    val senderCardScale by animateFloatAsState(
        when (syncState) {
            is SyncUIState.HalfDuplex -> 1.1f
            is SyncUIState.Running -> .8f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
    )

    val devicesContent = remember {
        movableContentOf {
            LocalDeviceUICard(
                device = currentDevice,
                platformOS = currentDevicePlatform,
                modifier = Modifier.widthIn(max = 300.dp)
                    .graphicsLayer {
                        scaleX = senderCardScale
                        scaleY = senderCardScale
                    },
            )
            Spacer(modifier = Modifier.height(24.dp))
            ReceiverDeviceUICard(
                device = externalDevice,
                modifier = Modifier.widthIn(max = 300.dp)
                    .graphicsLayer {
                        scaleX = receiverCardScale
                        scaleY = receiverCardScale
                    },
            )
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = contentPadding,
    ) {
        item {
            when {
                windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { devicesContent() }

                else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { devicesContent() }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Text(
                text = syncState.titleText,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = syncState.descText,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (syncState is SyncUIState.FullDuplex) item {
            Button(
                onClick = onCheckSketches,
                modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                shapes = ButtonDefaults.shapes(
                    shape = ButtonDefaults.elevatedShape,
                    pressedShape = ButtonDefaults.mediumPressedShape,
                ),
            ) {
                Text("Check Sketches", style = MaterialTheme.typography.titleMediumEmphasized)
            }
        }
    }
}


private val SyncUIState.titleText: String
    @Composable
    get() = when (this) {
        is SyncUIState.Failed -> "Sync Failed"
        SyncUIState.FullDuplex -> "Sync Completed"
        SyncUIState.HalfDuplex -> "Receiving data from other deivce"
        SyncUIState.NotRunning -> "Sync Not Started"
        SyncUIState.Running -> "Sending data to other deivce"
        SyncUIState.Started -> "Sync Started"
    }

private val SyncUIState.descText: String
    @Composable
    get() = when (this) {
        is SyncUIState.Failed -> "Failed :${this.message}"
        SyncUIState.FullDuplex -> "Sync completed both of the devices have exchanged the data"
        SyncUIState.HalfDuplex -> "Current device data is send waiting for data from other device"
        SyncUIState.NotRunning -> "Sync is not running"
        SyncUIState.Running -> "Sending data to the foreign device"
        SyncUIState.Started -> "Sync Started , waiting for handshake to complete"
    }
