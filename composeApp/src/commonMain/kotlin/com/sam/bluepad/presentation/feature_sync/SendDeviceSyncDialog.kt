package com.sam.bluepad.presentation.feature_sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.feature_sync.event.SendDeviceSyncScreenEvents
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.dialog_action_cancel
import com.sam.bluepad.resources.sync_device_dialog_text
import com.sam.bluepad.resources.sync_device_dialog_title
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

@Composable
fun SendSyncDialog(
	onEvent: (SendDeviceSyncScreenEvents) -> Unit,
	modifier: Modifier = Modifier,
	onCancel: () -> Unit = {},
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
		modifier = modifier.widthIn(min = Dimensions.DIALOG_MIN_WIDTH).width(IntrinsicSize.Max)

	) {
		Column(
			modifier = Modifier.padding(all = Dimensions.DIALOG_CONTENT_PADDING),
			horizontalAlignment = Alignment.Start
		) {
			Text(
				text = stringResource(Res.string.sync_device_dialog_title),
				style = MaterialTheme.typography.headlineSmallEmphasized,
				color = titleContentColor
			)

			Spacer(modifier = Modifier.height(16.dp))
			Text(
				text = stringResource(Res.string.sync_device_dialog_text),
				style = MaterialTheme.typography.bodyMedium,
				color = textContentColor,
			)

			Spacer(modifier = Modifier.padding(bottom = 24.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(
					onClick = onCancel,
					enabled = true
				) {
					Text(text = stringResource(Res.string.dialog_action_cancel))
				}
			}
		}
	}
}