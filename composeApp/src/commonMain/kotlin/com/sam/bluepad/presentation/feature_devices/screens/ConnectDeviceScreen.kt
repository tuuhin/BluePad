package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.ble.models.BLEConnectionState
import com.sam.bluepad.presentation.feature_devices.events.ConnectDeviceScreenEvent
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.dialog_action_add_device
import com.sam.bluepad.resources.dialog_action_disconnect
import com.sam.bluepad.resources.external_device_conn_device_data_found
import com.sam.bluepad.resources.external_device_conn_dialog_text
import com.sam.bluepad.resources.external_device_conn_dialog_title
import com.sam.bluepad.resources.external_device_conn_state_connected
import com.sam.bluepad.resources.external_device_conn_state_connecting
import com.sam.bluepad.resources.external_device_conn_state_disconnected
import com.sam.bluepad.resources.external_device_conn_state_disconnecting
import com.sam.bluepad.resources.ic_device_connecting
import com.sam.bluepad.resources.ic_device_success
import com.sam.bluepad.resources.ic_disconnected
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectDeviceScreen(
	connectionState: BLEConnectionState,
	onEvent: (ConnectDeviceScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	hasPeerDataFound: Boolean = false,
	textContentColor: Color = AlertDialogDefaults.textContentColor,
	onNavigateBack: @Composable () -> Unit = {},
) {
	val snackBarHostState = LocalSnackBarState.current
	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = stringResource(Res.string.external_device_conn_dialog_title)) },
				subtitle = { Text(text = stringResource(Res.string.external_device_conn_dialog_text)) },
				navigationIcon = onNavigateBack,
				scrollBehavior = topBarScrollBehaviour,
				actions = {
					TextButton(
						onClick = { onEvent(ConnectDeviceScreenEvent.OnDisconnect) },
						enabled = connectionState == BLEConnectionState.CONNECTED || connectionState == BLEConnectionState.CONNECTING,
						colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
						contentPadding = ButtonDefaults.MediumContentPadding
					) {
						Text(text = stringResource(Res.string.dialog_action_disconnect))
					}
				}
			)
		},
		floatingActionButton = {
			AnimatedVisibility(
				visible = connectionState == BLEConnectionState.CONNECTED || hasPeerDataFound,
				enter = slideInVertically(),
				exit = slideOutVertically()
			) {
				ExtendedFloatingActionButton(
					onClick = { onEvent(ConnectDeviceScreenEvent.OnSaveDevice) },
				) {
					if (hasPeerDataFound) {
						Text(text = stringResource(Res.string.dialog_action_add_device))
					} else {
						CircularProgressIndicator(
							modifier = Modifier.size(24.dp),
							color = MaterialTheme.colorScheme.outline
						)
						Spacer(modifier = Modifier.width(6.dp))
						Text(text = "Retrieving")
					}
				}
			}
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		Column(
			modifier = Modifier.fillMaxSize()
				.padding(padding)
				.padding(
					horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
					vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING
				)
		) {
			Crossfade(
				targetState = hasPeerDataFound,
				animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
			) { hasPeer ->
				Column(
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally,
					modifier = Modifier.fillMaxSize()
				) {
					if (hasPeer) {
						Icon(
							painter = painterResource(Res.drawable.ic_device_success),
							contentDescription = null,
							modifier = Modifier.size(128.dp),
							tint = MaterialTheme.colorScheme.surfaceTint
						)
						Spacer(modifier = Modifier.height(12.dp))
						Text(
							text = stringResource(Res.string.external_device_conn_device_data_found),
							style = MaterialTheme.typography.bodyLargeEmphasized,
							color = textContentColor
						)
					} else {
						Icon(
							painter = if (connectionState != BLEConnectionState.DISCONNECTED)
								painterResource(Res.drawable.ic_device_connecting)
							else painterResource(Res.drawable.ic_disconnected),
							contentDescription = null,
							modifier = Modifier.size(128.dp),
							tint = MaterialTheme.colorScheme.surfaceTint
						)
						Spacer(modifier = Modifier.height(12.dp))
						Text(
							text = connectionState.readableString,
							style = MaterialTheme.typography.bodyLargeEmphasized,
							color = textContentColor
						)
					}
				}
			}
		}
	}
}

private val BLEConnectionState.readableString: String
	@Composable
	get() = when (this) {
		BLEConnectionState.CONNECTING -> stringResource(Res.string.external_device_conn_state_connecting)
		BLEConnectionState.CONNECTED -> stringResource(Res.string.external_device_conn_state_connected)
		BLEConnectionState.DISCONNECTING -> stringResource(Res.string.external_device_conn_state_disconnecting)
		BLEConnectionState.DISCONNECTED -> stringResource(Res.string.external_device_conn_state_disconnected)
	}

private class BLEConnectionStateParams : CollectionPreviewParameterProvider<BLEConnectionState>(
	BLEConnectionState.entries
)

@Preview
@Composable
fun ConnectDeviceScreenPreview(
	@PreviewParameter(BLEConnectionStateParams::class)
	state: BLEConnectionState
) =
	BluePadTheme {
		ConnectDeviceScreen(
			connectionState = state,
			onEvent = {},
		)
	}