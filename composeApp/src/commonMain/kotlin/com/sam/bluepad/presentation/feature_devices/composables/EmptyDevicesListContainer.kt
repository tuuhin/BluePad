package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_no_devices
import org.jetbrains.compose.resources.painterResource

@Composable
fun EmptyDevicesListContainer(
	onAddDevice: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Image(
			painter = painterResource(Res.drawable.ic_no_devices),
			contentDescription = "No devices present",
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
		)
		Spacer(modifier = Modifier.height(12.dp))
		ElevatedButton(
			onClick = onAddDevice,
			modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
			contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
			shapes = ButtonDefaults.shapes(
				shape = ButtonDefaults.elevatedShape,
				pressedShape = ButtonDefaults.mediumPressedShape
			)
		) {
			Text("Add device")
		}
	}
}