package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.presentation.feature_sync.state.SyncUIState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_arrow_direction_receieve
import com.sam.bluepad.resources.ic_arrow_direction_send
import com.sam.bluepad.resources.ic_receiver_action_reset
import org.jetbrains.compose.resources.painterResource

@Composable
fun ReceiverSyncingDataContainer(
    externalDevice: ExternalDeviceModel,
    currentDevice: LocalDeviceInfoModel,
    onCheckSketches: () -> Unit,
    onDisconnectAndReset: () -> Unit,
    modifier: Modifier = Modifier,
    currentDevicePlatform: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    syncState: SyncUIState = SyncUIState.NotRunning,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = contentPadding,
    ) {
        item {
            ReceiverCardUIHeroContainer(
                externalDevice = externalDevice,
                currentDevice = currentDevice,
                currentDevicePlatform = currentDevicePlatform,
                syncState = syncState,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = syncState.receiverTitleText,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = syncState.receiverDescText,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (syncState is SyncUIState.FullSyncSuccessFull) {
            item {
                Modifier.height(24.dp)
                Row(
                    modifier = Modifier.animateItem(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDisconnectAndReset,
                        modifier = Modifier.minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        shape = IconButtonDefaults.largeRoundShape,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_receiver_action_reset),
                            contentDescription = "Reset Receiver",
                        )
                    }

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
    }
}

@Composable
private fun ReceiverCardUIHeroContainer(
    externalDevice: ExternalDeviceModel,
    currentDevice: LocalDeviceInfoModel,
    modifier: Modifier = Modifier,
    currentDevicePlatform: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    syncState: SyncUIState = SyncUIState.NotRunning,
) {
    val windowInfo = currentWindowAdaptiveInfo()
    val isLargeWindow = windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)


    val receiverCardScale by animateFloatAsState(
        when (syncState) {
            is SyncUIState.Running -> 1.1f
            is SyncUIState.HalfDuplexCompleted -> .8f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
    )

    val senderCardScale by animateFloatAsState(
        when (syncState) {
            is SyncUIState.HalfDuplexCompleted -> 1.1f
            is SyncUIState.Running -> .8f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
    )

    val devicesContent = remember {
        movableContentOf { syncUiState: SyncUIState, isLarge: Boolean ->
            LocalDeviceUICard(
                device = currentDevice,
                platformOS = currentDevicePlatform,
                modifier = Modifier.widthIn(max = 300.dp)
                    .graphicsLayer {
                        scaleX = senderCardScale
                        scaleY = senderCardScale
                    },
            )

            AnimatedContent(
                targetState = syncUiState,
                contentAlignment = Alignment.Center,
            ) { state ->
                when (state) {
                    SyncUIState.Running -> Icon(
                        painter = painterResource(Res.drawable.ic_arrow_direction_send),
                        contentDescription = "Action send",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                            .graphicsLayer {
                                if (!isLarge) rotationZ = 90f
                                if (!isLarge) translationY = (-20).dp.toPx()
                                if (isLarge) translationX = (-20).dp.toPx()
                            },
                    )

                    SyncUIState.HalfDuplexCompleted -> Icon(
                        painter = painterResource(Res.drawable.ic_arrow_direction_receieve),
                        contentDescription = "Action send",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                            .graphicsLayer {
                                if (!isLarge) rotationZ = 90f
                                if (!isLarge) translationY = 20.dp.toPx()
                                if (isLarge) translationX = 20.dp.toPx()
                            },
                    )

                    SyncUIState.FullSyncSuccessFull -> {
                        Text(
                            text = "Synced",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                        )
                    }

                    else -> {}
                }
            }
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


    Box(modifier = modifier) {
        when {
            isLargeWindow -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                devicesContent(syncState, true)
            }

            else -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                devicesContent(syncState, false)
            }
        }
    }
}
