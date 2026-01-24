package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_start_scan
import com.sam.bluepad.resources.action_stop_scan
import org.jetbrains.compose.resources.stringResource

@Composable
fun BLEScanStartStopButton(
	onStartScan: () -> Unit,
	onStopScan: () -> Unit,
	modifier: Modifier = Modifier,
	isScanning: Boolean = false,
	enabled: Boolean = true,
) {

	Button(
		onClick = { if (isScanning) onStopScan() else onStartScan() },
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