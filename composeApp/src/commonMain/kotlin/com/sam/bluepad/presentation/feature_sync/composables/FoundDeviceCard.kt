package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.DeviceOSTypeContainer
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_revoke_device
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

@Composable
fun FoundDeviceCard(
	device: ExternalDeviceModel,
	modifier: Modifier = Modifier,
	isSelected: Boolean = false,
	onSelectDevice: () -> Unit = {},
	shape: Shape = MaterialTheme.shapes.extraLarge,
) {
	Card(
		modifier = modifier
			.heightIn(min = Dimensions.EXTERNAL_DEVICE_CARD_MIN_HEIGHT),
		shape = shape,
		border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(all = Dimensions.CARD_INTERNAL_PADDING),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			DeviceOSTypeContainer(deviceOs = device.deviceOs)
			Spacer(modifier = Modifier.height(6.dp))
			Text(
				text = device.displayName ?: "N/A",
				style = MaterialTheme.typography.titleLargeEmphasized,
				color = MaterialTheme.colorScheme.primary,
				fontWeight = FontWeight.SemiBold,
			)
			Button(
				onClick = onSelectDevice,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.secondaryContainer,
					contentColor = MaterialTheme.colorScheme.onSecondaryContainer
				),
				shapes = ButtonDefaults.shapes(
					shape = ButtonDefaults.shape,
					pressedShape = ButtonDefaults.mediumPressedShape
				),
				contentPadding = ButtonDefaults.SmallContentPadding,
				modifier = Modifier.weight(1f)
			) {
				Text(
					text = stringResource(Res.string.action_revoke_device),
					style = MaterialTheme.typography.titleMedium
				)
			}
		}
	}
}