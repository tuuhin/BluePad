package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.receiver_sync_request_allow
import com.sam.bluepad.resources.receiver_sync_request_deny
import com.sam.bluepad.theme.BluePadTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReceiverFoundContainer(
    externalDevice: ExternalDeviceModel,
    onStartSync: () -> Unit,
    onRejectDevice: () -> Unit,
    modifier: Modifier = Modifier,
    currentDevice: ExternalDeviceModel? = null,
    isSyncStarted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues.Zero,
) {
    val windowInfo = currentWindowAdaptiveInfo()

    val devicesContent = remember {
        movableContentOf {
            ReceiverDeviceUICard(
                device = externalDevice,
                isRemoteDevice = true,
                localDeviceContainerColor = MaterialTheme.colorScheme.primaryContainer,
            )
            currentDevice?.let { device ->
                ReceiverDeviceUICard(
                    device = device,
                    localDeviceContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
        }
    }

    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                devicesContent()
            }
        else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            devicesContent()
        }


        Spacer(modifier = Modifier.height(12.dp))
        // two action allow and reject
        SyncPermissionButtons(
            showOptions = !isSyncStarted,
            onStartSync = onStartSync,
            onRejectSync = onRejectDevice,
        )
    }
}


@Composable
private fun SyncPermissionButtons(
    showOptions: Boolean,
    onStartSync: () -> Unit,
    onRejectSync: () -> Unit,
    modifier: Modifier = Modifier,
    primaryActionColor: Color = MaterialTheme.colorScheme.primary,
    secondaryActionColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    AnimatedVisibility(
        visible = showOptions,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(min = 120.dp, max = 320.dp)
        ) {
            Button(
                onClick = onStartSync,
                shapes = ButtonDefaults.shapes(
                    shape = ButtonDefaults.shape,
                    pressedShape = ButtonDefaults.mediumPressedShape
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryActionColor,
                    contentColor = contentColorFor(primaryActionColor)
                ),
                enabled = showOptions,
                contentPadding = ButtonDefaults.MediumContentPadding,
                modifier = Modifier.weight(.5f),
            ) {
                Text(
                    text = stringResource(Res.string.receiver_sync_request_allow),
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
            Button(
                onClick = onRejectSync,
                shapes = ButtonDefaults.shapes(
                    shape = ButtonDefaults.shape, pressedShape = ButtonDefaults.mediumPressedShape
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = secondaryActionColor,
                    contentColor = contentColorFor(secondaryActionColor),
                ),
                enabled = showOptions,
                contentPadding = ButtonDefaults.MediumContentPadding,
                modifier = Modifier.weight(.5f),
            ) {
                Text(
                    text = stringResource(Res.string.receiver_sync_request_deny),
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}

@Preview
@Preview(device = Devices.DESKTOP)
@Composable
private fun ReceiverFoundContainerPreview() = BluePadTheme {
    ReceiverFoundContainer(
        externalDevice = PreviewFakes.FAKE_EXTERNAL_MODEL,
        currentDevice = PreviewFakes.FAKE_EXTERNAL_MODEL_2,
        onStartSync = {},
        onRejectDevice = {},
        contentPadding = PaddingValues(12.dp),
    )
}