package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_warning
import com.sam.bluepad.resources.scan_results_save_device_warning
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MultipleDeviceWarning(
	modifier: Modifier = Modifier,
	showWarning: Boolean = false,
	containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
	contentColor: Color = contentColorFor(containerColor),
	shape: Shape = MaterialTheme.shapes.extraLarge,
) {
	AnimatedVisibility(
		visible = showWarning,
		modifier = modifier,
		enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()) + slideInVertically(),
		exit = fadeOut(MaterialTheme.motionScheme.defaultSpatialSpec()) + slideOutVertically(),
	) {
		Card(
			colors = CardDefaults.cardColors(
				containerColor = containerColor,
				contentColor = contentColor
			),
			shape = shape,
		) {
			Row(
				modifier = Modifier.padding(all = Dimensions.CARD_INTERNAL_PADDING),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Box(
					modifier = Modifier
						.sizeIn(minWidth = 36.dp, minHeight = 36.dp)
						.background(
							color = contentColor,
							shape = MaterialTheme.shapes.extraLarge
						),
					contentAlignment = Alignment.Center,
				) {
					Icon(
						painter = painterResource(Res.drawable.ic_warning),
						contentDescription = stringResource(Res.string.scan_results_save_device_warning),
						tint = containerColor,
						modifier = Modifier.size(24.dp)
					)
				}
				Text(
					text = stringResource(Res.string.scan_results_save_device_warning),
					style = MaterialTheme.typography.labelMediumEmphasized,
					modifier = Modifier
						.weight(1f)
						.padding(horizontal = 4.dp)
				)
			}
		}
	}
}