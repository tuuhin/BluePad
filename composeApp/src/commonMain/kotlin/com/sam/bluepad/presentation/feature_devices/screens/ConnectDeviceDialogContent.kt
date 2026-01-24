package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.presentation.feature_devices.composables.ConnectDeviceDialogActions
import com.sam.bluepad.presentation.feature_devices.composables.ConnectionStateContent
import com.sam.bluepad.presentation.feature_devices.events.ConnectDeviceScreenEvent
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.external_device_conn_dialog_text
import com.sam.bluepad.resources.external_device_conn_dialog_title
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectDeviceDialogContent(
	onEvent: (ConnectDeviceScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	connectionState: BLEConnectionState = BLEConnectionState.CONNECTING,
	connectedPeerData: BLEPeerData? = null,
	errorMessage: String? = null,
	shape: Shape = AlertDialogDefaults.shape,
	containerColor: Color = AlertDialogDefaults.containerColor,
	tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
	titleContentColor: Color = AlertDialogDefaults.titleContentColor,
	textContentColor: Color = AlertDialogDefaults.textContentColor,
	shadowElevation: Dp = 0.dp,
) {
	Surface(
		shape = shape,
		color = containerColor,
		tonalElevation = tonalElevation,
		shadowElevation = shadowElevation,
		modifier = modifier.widthIn(min = Dimensions.DIALOG_MIN_WIDTH)
			.width(IntrinsicSize.Max)
	) {
		Column(
			modifier = Modifier.padding(all = Dimensions.DIALOG_CONTENT_PADDING),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = stringResource(Res.string.external_device_conn_dialog_title),
				style = MaterialTheme.typography.headlineSmallEmphasized,
				color = titleContentColor
			)
			Spacer(modifier = Modifier.height(Dimensions.DIALOG_SECTIONS_PADDING))
			Text(
				text = stringResource(Res.string.external_device_conn_dialog_text),
				style = MaterialTheme.typography.bodyMediumEmphasized,
				color = textContentColor,
				textAlign = TextAlign.Center
			)
			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
			ConnectionStateContent(
				connectionState = connectionState,
				peerData = connectedPeerData,
				textStyle = MaterialTheme.typography.labelMediumEmphasized,
				modifier = Modifier.fillMaxWidth()
			)
			AnimatedVisibility(
				visible = errorMessage != null,
				enter = slideInVertically() + fadeIn(),
				exit = slideOutVertically() + fadeOut(),
				modifier = Modifier.align(Alignment.CenterHorizontally)
			) {
				errorMessage?.let { message ->
					Text(
						text = message,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.error,
						modifier = Modifier.padding(vertical = 4.dp)
					)
				}
			}
			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
			ConnectDeviceDialogActions(
				isConnected = connectionState == BLEConnectionState.CONNECTED,
				isDisconnected = connectionState == BLEConnectionState.DISCONNECTED,
				hasPeerData = connectedPeerData != null,
				onRetryConnection = { onEvent(ConnectDeviceScreenEvent.OnRetryConnection) },
				onSaveDevice = { onEvent(ConnectDeviceScreenEvent.OnSaveDevice) },
				onDisconnect = { onEvent(ConnectDeviceScreenEvent.OnDisconnect) },
				modifier = Modifier.align(Alignment.End)
			)
		}
	}
}

private class BLEConnectionStateParams : PreviewParameterProvider<BLEConnectionState> {
	override val values: Sequence<BLEConnectionState>
		get() = BLEConnectionState.entries.asSequence()
}

@Preview
@Composable
fun ConnectDeviceDialogContentWithConnectedPeerPreview(
	@PreviewParameter(BLEConnectionStateParams::class)
	state: BLEConnectionState
) = BluePadTheme {
	ConnectDeviceDialogContent(
		connectionState = state,
		connectedPeerData = PreviewFakes.FAKE_BLE_PEER_MODEL,
		onEvent = {},
	)
}

@Preview
@Composable
fun ConnectDeviceDialogContentWithoutConnectedPeerPreview(
	@PreviewParameter(BLEConnectionStateParams::class)
	state: BLEConnectionState
) =
	BluePadTheme {
		ConnectDeviceDialogContent(
			connectionState = state,
			onEvent = {},
		)
	}

