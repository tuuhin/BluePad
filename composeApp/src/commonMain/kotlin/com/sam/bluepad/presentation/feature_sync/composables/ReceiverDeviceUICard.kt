package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.DeviceOSTypeContainer
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions

@Composable
fun ReceiverDeviceUICard(
    device: ExternalDeviceModel,
    modifier: Modifier = Modifier,
    isRemoteDevice: Boolean = false,
    localDeviceContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    remoteDeviceContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    shape: Shape = MaterialTheme.shapes.extraLarge,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isRemoteDevice) remoteDeviceContainerColor else localDeviceContainerColor,
            contentColor = if (isRemoteDevice) contentColorFor(remoteDeviceContainerColor)
            else contentColorFor(localDeviceContainerColor),
        ),
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.CARD_INTERNAL_PADDING_LARGE)
                .width(IntrinsicSize.Max)
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AssistChip(
                    onClick = {},
                    label = { if (isRemoteDevice) Text(text = "Remote") else Text("Local") },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer),
                    shape = MaterialTheme.shapes.extraLarge,
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
                DeviceOSTypeContainer(
                    deviceOs = device.deviceOs,
                    modifier = Modifier.size(64.dp),
                    containerColor = if (isRemoteDevice) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = device.displayName ?: "Unknown",
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )
                Text(
                    text = buildAnnotatedString {
                        append("ID: ")
                        withStyle(style = SpanStyle(fontFamily = FontFamily.Monospace)) {
                            append(device.id.toHexString())
                        }
                    },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        }
    }
}

private class IsRemoteDevicePreviewParameters : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean>
        get() = sequenceOf(true, false)
}

@Preview
@Composable
private fun ReceiverDeviceUICardPreview(
    @PreviewParameter(IsRemoteDevicePreviewParameters::class)
    isRemoteDevice: Boolean,
) = BluePadTheme {
    Surface(color = MaterialTheme.colorScheme.background) {
        ReceiverDeviceUICard(
            device = PreviewFakes.FAKE_EXTERNAL_MODEL,
            isRemoteDevice = isRemoteDevice
        )
    }
}