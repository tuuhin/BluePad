package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_revoke_device
import com.sam.bluepad.resources.action_sync_device
import com.sam.bluepad.resources.ic_os_android
import com.sam.bluepad.resources.ic_os_unknown
import com.sam.bluepad.resources.ic_os_windows
import com.sam.bluepad.resources.scan_result_device_name_title
import com.sam.bluepad.resources.scan_results_save_device_warning
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExternalDeviceCard(
	device: ExternalDeviceModel,
	onSync: () -> Unit,
	onRevoke: () -> Unit,
	modifier: Modifier = Modifier,
	isActionEnabled: Boolean = true,
	shape: Shape = MaterialTheme.shapes.large,
) {
	Card(
		modifier = modifier.heightIn(min = Dimensions.EXTERNAL_DEVICE_CARD_MIN_HEIGHT),
		shape = shape,
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(all = Dimensions.CARD_INTERNAL_PADDING),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(18.dp),
				modifier = Modifier.fillMaxWidth()
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = stringResource(Res.string.scan_result_device_name_title),
						style = MaterialTheme.typography.titleMediumEmphasized,
						fontWeight = FontWeight.SemiBold,
						color = MaterialTheme.colorScheme.onSurface
					)
					Text(
						text = device.displayName ?: "N/A",
						style = MaterialTheme.typography.titleLargeEmphasized,
						color = MaterialTheme.colorScheme.primary,
						fontWeight = FontWeight.SemiBold,
					)
				}
				DeviceOSTypeContainer(deviceOs = device.deviceOs)
			}
			HorizontalDivider()
			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.align(Alignment.End),
			) {
				Button(
					onClick = onRevoke,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer,
						contentColor = MaterialTheme.colorScheme.onSecondaryContainer
					),
					shapes = ButtonDefaults.shapes(
						shape = ButtonDefaults.shape,
						pressedShape = ButtonDefaults.mediumPressedShape
					),
					enabled = isActionEnabled,
					contentPadding = ButtonDefaults.SmallContentPadding,
					modifier = Modifier.weight(1f)
				) {
					Text(
						text = stringResource(Res.string.action_revoke_device),
						style = MaterialTheme.typography.titleMedium
					)
				}
				Button(
					onClick = onSync,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.primaryContainer,
						contentColor = MaterialTheme.colorScheme.onPrimaryContainer
					),
					shapes = ButtonDefaults.shapes(
						shape = ButtonDefaults.shape,
						pressedShape = ButtonDefaults.mediumPressedShape
					),
					enabled = isActionEnabled,
					contentPadding = ButtonDefaults.SmallContentPadding,
					modifier = Modifier.weight(1f)
				) {
					Text(
						text = stringResource(Res.string.action_sync_device),
						style = MaterialTheme.typography.titleMedium
					)
				}
			}
		}
	}
}

@Composable
private fun DeviceOSTypeContainer(
	deviceOs: DevicePlatformOS,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier
			.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
			.background(
				color = MaterialTheme.colorScheme.primaryContainer,
				shape = when (deviceOs) {
					DevicePlatformOS.ANDROID -> MaterialShapes.Cookie4Sided.toShape()
					DevicePlatformOS.WINDOWS -> MaterialShapes.Square.toShape()
					DevicePlatformOS.UNKNOWN -> MaterialShapes.Arch.toShape()
				},
			),
		contentAlignment = Alignment.Center,
	) {
		Icon(
			painter = when (deviceOs) {
				DevicePlatformOS.ANDROID -> painterResource(Res.drawable.ic_os_android)
				DevicePlatformOS.WINDOWS -> painterResource(Res.drawable.ic_os_windows)
				DevicePlatformOS.UNKNOWN -> painterResource(Res.drawable.ic_os_unknown)
			},
			contentDescription = stringResource(Res.string.scan_results_save_device_warning),
			tint = MaterialTheme.colorScheme.onPrimaryContainer,
			modifier = Modifier.size(24.dp)
		)
	}
}

@Preview
@Composable
private fun ExternalDeviceCardPreview() = BluePadTheme {
	ExternalDeviceCard(device = PreviewFakes.FAKE_EXTERNAL_MODEL, onSync = {}, onRevoke = {})
}