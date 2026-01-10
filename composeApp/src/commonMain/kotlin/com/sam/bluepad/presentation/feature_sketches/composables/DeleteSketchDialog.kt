package com.sam.bluepad.presentation.feature_sketches.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_delete
import com.sam.bluepad.resources.delete_sketch_dialog_text
import com.sam.bluepad.resources.delete_sketch_dialog_title
import com.sam.bluepad.resources.dialog_action_cancel
import com.sam.bluepad.resources.ic_warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteSketchDialog(
	showDialog: Boolean,
	onConfirm: () -> Unit,
	onCancel: () -> Unit,
	modifier: Modifier = Modifier
) {

	if (!showDialog) return

	AlertDialog(
		onDismissRequest = onCancel,
		modifier = modifier,
		confirmButton = {
			Button(
				onClick = onConfirm,
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.errorContainer,
					contentColor = MaterialTheme.colorScheme.onErrorContainer
				),
				contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.ExtraSmallContainerHeight)
			) {
				Text(
					stringResource(Res.string.action_delete)
				)
			}
		},
		dismissButton = {
			TextButton(onClick = onCancel) {
				Text(text = stringResource(Res.string.dialog_action_cancel))
			}
		},
		icon = {
			Icon(
				painter = painterResource(Res.drawable.ic_warning),
				contentDescription = null
			)
		},
		title = { Text(text = stringResource(Res.string.delete_sketch_dialog_title)) },
		text = { Text(text = stringResource(Res.string.delete_sketch_dialog_text)) },
	)
}