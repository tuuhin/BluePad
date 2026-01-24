package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.dialog_action_add_device
import com.sam.bluepad.resources.dialog_action_disconnect
import com.sam.bluepad.resources.external_device_conn_retry_connection
import com.sam.bluepad.resources.external_device_conn_state_connecting
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConnectDeviceDialogActions(
	onDisconnect: () -> Unit,
	onSaveDevice: () -> Unit,
	onRetryConnection: () -> Unit,
	modifier: Modifier = Modifier,
	hasPeerData: Boolean = false,
	isConnected: Boolean = false,
	isDisconnected: Boolean = false,
) {
	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.spacedBy(Dimensions.DIALOG_ACTIONS_SPACING),
		verticalAlignment = Alignment.CenterVertically
	) {
		TextButton(
			onClick = onDisconnect,
			enabled = isConnected,
			colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
		) {
			Text(
				text = stringResource(Res.string.dialog_action_disconnect),
				style = MaterialTheme.typography.labelMediumEmphasized
			)
		}
		Button(
			onClick = {
				if (hasPeerData) onSaveDevice()
				else if (isDisconnected) onRetryConnection()
			},
			enabled = hasPeerData || isDisconnected,
			colors = ButtonDefaults.buttonColors(
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
				contentColor = MaterialTheme.colorScheme.onSecondaryContainer
			),
		) {
			when {
				hasPeerData -> Text(text = stringResource(Res.string.dialog_action_add_device))
				isDisconnected -> Text(text = stringResource(Res.string.external_device_conn_retry_connection))
				else -> Text(text = stringResource(Res.string.external_device_conn_state_connecting))
			}
		}
	}
}