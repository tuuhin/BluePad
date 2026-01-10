package com.sam.bluepad.presentation.feature_settings.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_rename
import com.sam.bluepad.resources.action_update
import com.sam.bluepad.resources.dialog_action_cancel
import com.sam.bluepad.resources.settings_device_name_title
import com.sam.bluepad.resources.settings_device_rename_dialog_text
import com.sam.bluepad.resources.settings_device_rename_dialog_title
import com.sam.bluepad.theme.Dimensions
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDeviceNameListItem(
	currentName: String,
	onUpdateName: (String) -> Unit,
	modifier: Modifier = Modifier,
) {

	var showDialog by rememberSaveable { mutableStateOf(false) }

	ListItem(
		headlineContent = { Text(text = stringResource(Res.string.settings_device_name_title)) },
		supportingContent = { Text(text = currentName) },
		trailingContent = {
			Button(
				onClick = { showDialog = true },
				shape = ButtonDefaults.elevatedShape
			) {
				Text(text = stringResource(Res.string.action_update))
			}
		},
		colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
		modifier = modifier.clip(MaterialTheme.shapes.medium),
	)

	if (!showDialog) return


	BasicAlertDialog(onDismissRequest = { showDialog = false }) {
		UpdateDeviceNameDialogContent(
			onDismissDialog = { showDialog = false },
			onConfirmName = { newName ->
				onUpdateName(newName)
				showDialog = false
			},
		)
	}
}

@Composable
fun UpdateDeviceNameDialogContent(
	onDismissDialog: () -> Unit,
	onConfirmName: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	val textFieldState = rememberTextFieldState()
	var hasError by remember { mutableStateOf(false) }
	var errorMessage by remember { mutableStateOf("") }

	LaunchedEffect(textFieldState) {
		snapshotFlow { textFieldState.text }.onEach {
			if (hasError) {
				hasError = false
				errorMessage = ""
			}
		}.launchIn(this)
	}

	Surface(
		color = AlertDialogDefaults.containerColor,
		contentColor = AlertDialogDefaults.containerColor,
		tonalElevation = AlertDialogDefaults.TonalElevation,
		shadowElevation = 0.dp,
		shape = AlertDialogDefaults.shape,
		modifier = modifier,
	) {
		Column(
			modifier = Modifier.padding(all = Dimensions.DIALOG_CONTENT_PADDING)
		) {
			Text(
				text = stringResource(Res.string.settings_device_rename_dialog_title),
				style = MaterialTheme.typography.headlineSmall,
				color = AlertDialogDefaults.titleContentColor,
				modifier = Modifier.padding(16.dp)
			)
			Text(
				text = stringResource(Res.string.settings_device_rename_dialog_text),
				color = AlertDialogDefaults.textContentColor,
				style = MaterialTheme.typography.bodyMedium,
				modifier = Modifier.padding(vertical = 6.dp),
			)
			OutlinedTextField(
				state = textFieldState,
				label = { Text(text = "New Name") },
				shape = MaterialTheme.shapes.medium,
				onKeyboardAction = {},
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.Text,
					imeAction = ImeAction.Done
				),
				modifier = Modifier.fillMaxWidth()
			)
			AnimatedVisibility(
				visible = hasError,
				enter = slideInVertically(MaterialTheme.motionScheme.slowEffectsSpec()),
				exit = slideOutVertically(MaterialTheme.motionScheme.slowEffectsSpec())
			) {
				Text(
					text = errorMessage,
					style = MaterialTheme.typography.labelMedium,
					color = MaterialTheme.colorScheme.error
				)
			}
			Spacer(modifier = Modifier.height(12.dp))
			Row(
				modifier = Modifier.align(Alignment.End),
				horizontalArrangement = Arrangement.spacedBy(6.dp)
			) {
				TextButton(onClick = onDismissDialog) {
					Text(text = stringResource(Res.string.dialog_action_cancel))
				}
				Button(
					onClick = {
						if (textFieldState.text.isNotEmpty()) {
							onConfirmName(textFieldState.text.toString())
							onDismissDialog()
						} else {
							hasError = true
							errorMessage = "Empty name not allowed"
						}
					},
					enabled = textFieldState.text.isNotEmpty(),
					shape = MaterialTheme.shapes.large
				) {
					Text(text = stringResource(Res.string.action_rename))
				}
			}
		}
	}
}