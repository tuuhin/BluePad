package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import com.sam.bluepad.domain.ble.models.BLEPeerSignalStrength
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.scan_device_signal_strength_avg
import com.sam.bluepad.resources.scan_device_signal_strength_excellent
import com.sam.bluepad.resources.scan_device_signal_strength_good
import com.sam.bluepad.resources.scan_device_signal_strength_poor
import com.sam.bluepad.resources.scan_device_signal_strength_unreliable
import com.sam.bluepad.resources.scan_device_signal_strength_very_poor
import com.sam.bluepad.resources.scan_result_device_address_tittle
import com.sam.bluepad.resources.scan_result_device_name_not_found
import com.sam.bluepad.resources.scan_result_device_name_title
import com.sam.bluepad.resources.scan_result_device_signal_strength_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScanDeviceCard(
	device: BLEPeerDevice,
	onConnect: () -> Unit,
	modifier: Modifier = Modifier,
	isActionEnabled: Boolean = true,
	shape: Shape = MaterialTheme.shapes.large,
) {

	Card(
		modifier = modifier,
		shape = shape,
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
				modifier = Modifier.fillMaxWidth()
			) {
				Text(
					text = stringResource(Res.string.scan_result_device_name_title),
					style = MaterialTheme.typography.titleMediumEmphasized,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = device.bleDeviceName
						?: stringResource(Res.string.scan_result_device_name_not_found),
					style = MaterialTheme.typography.bodyMediumEmphasized,
					color = if (device.bleDeviceName == null) MaterialTheme.colorScheme.tertiary
					else MaterialTheme.colorScheme.secondary
				)
			}
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
				modifier = Modifier.fillMaxWidth()
			) {
				Text(
					text = stringResource(Res.string.scan_result_device_address_tittle),
					style = MaterialTheme.typography.titleMediumEmphasized,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.primary
				)
				Text(
					text = device.deviceAddress,
					style = MaterialTheme.typography.bodyMediumEmphasized,
					fontFamily = FontFamily.Monospace,
					color = MaterialTheme.colorScheme.secondary
				)
			}
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
				modifier = Modifier.fillMaxWidth()
			) {
				Text(
					text = stringResource(Res.string.scan_result_device_signal_strength_title),
					style = MaterialTheme.typography.titleMediumEmphasized,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.primary
				)
				DeviceSignalStrength(device = device)
			}
			HorizontalDivider()
			Button(
				onClick = onConnect,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.secondaryContainer,
					contentColor = MaterialTheme.colorScheme.onSecondaryContainer
				),
				shapes = ButtonDefaults.shapes(
					shape = ButtonDefaults.shape,
					pressedShape = ButtonDefaults.mediumPressedShape
				),
				enabled = isActionEnabled,
				modifier = Modifier.widthIn(min = 120.dp)
					.align(Alignment.CenterHorizontally),
			) {
				Text(text = "Connect")
			}
		}
	}
}

@Composable
private fun DeviceSignalStrength(
	device: BLEPeerDevice,
	modifier: Modifier = Modifier
) {

	val power by remember(device.rssi) { derivedStateOf { device.signalStrength } }
	val powerAnimatedColor by animateColorAsState(
		targetValue = power.color,
		animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
	)

	Row(
		horizontalArrangement = Arrangement.spacedBy(4.dp),
		verticalAlignment = Alignment.CenterVertically, modifier = modifier,
	) {
		Box(
			modifier = Modifier
				.size(16.dp)
				.shadow(
					elevation = 8.dp,
					shape = MaterialTheme.shapes.large,
					ambientColor = powerAnimatedColor,
					spotColor = powerAnimatedColor
				)
				.background(powerAnimatedColor, CircleShape)
		)
		Text(
			text = power.readableString,
			style = MaterialTheme.typography.bodyMediumEmphasized,
			color = MaterialTheme.colorScheme.secondary
		)
	}
}

private val BLEPeerSignalStrength.readableString: String
	@Composable
	get() = when (this) {
		BLEPeerSignalStrength.EXCELLENT -> stringResource(Res.string.scan_device_signal_strength_excellent)
		BLEPeerSignalStrength.GOOD -> stringResource(Res.string.scan_device_signal_strength_good)
		BLEPeerSignalStrength.AVG -> stringResource(Res.string.scan_device_signal_strength_avg)
		BLEPeerSignalStrength.POOR -> stringResource(Res.string.scan_device_signal_strength_poor)
		BLEPeerSignalStrength.VER_POOR -> stringResource(Res.string.scan_device_signal_strength_very_poor)
		BLEPeerSignalStrength.UN_RELIABLE -> stringResource(Res.string.scan_device_signal_strength_unreliable)
	}

private val BLEPeerSignalStrength.color: Color
	@Composable
	get() = when (this) {
		BLEPeerSignalStrength.EXCELLENT -> Color(0xFF00E676)
		BLEPeerSignalStrength.GOOD -> Color(0xFF66BB6A)
		BLEPeerSignalStrength.AVG -> Color(0xFFFFCA28)
		BLEPeerSignalStrength.POOR -> Color(0xFFFF7043)
		BLEPeerSignalStrength.VER_POOR -> Color(0xFFEF5350)
		BLEPeerSignalStrength.UN_RELIABLE -> Color(0xFF9E9E9E)
	}