package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_start_scan
import com.sam.bluepad.resources.action_stop_scan
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun BLEScanStartStopButton(
	onStartScan: () -> Unit,
	onStopScan: () -> Unit,
	modifier: Modifier = Modifier,
	isScanning: Boolean = false,
	enabled: Boolean = true,
) {
	val permissionController = koinInject<PermissionsController>()
	val scope = rememberCoroutineScope()

	Button(
		onClick = {
			scope.launch {
				when (permissionController.getPermissionState(Permission.BLUETOOTH_LE)) {
					PermissionState.NotGranted, PermissionState.NotDetermined, PermissionState.Denied -> {
						permissionController.providePermission(Permission.BLUETOOTH_LE)
					}

					PermissionState.DeniedAlways -> permissionController.openAppSettings()
					PermissionState.Granted -> if (isScanning) onStopScan() else onStartScan()
				}
			}
		},
		shapes = ButtonDefaults.shapes(
			shape = ButtonDefaults.elevatedShape,
			pressedShape = ButtonDefaults.mediumPressedShape
		),
		colors = ButtonDefaults.buttonColors(
			containerColor = if (isScanning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
			contentColor = if (isScanning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
		),
		enabled = enabled,
		contentPadding = ButtonDefaults.SmallContentPadding,
		modifier = modifier,
	) {
		if (isScanning) Text(
			text = stringResource(Res.string.action_stop_scan),
			style = MaterialTheme.typography.labelLarge
		)
		else Text(
			text = stringResource(Res.string.action_start_scan),
			style = MaterialTheme.typography.labelLarge
		)
	}
}