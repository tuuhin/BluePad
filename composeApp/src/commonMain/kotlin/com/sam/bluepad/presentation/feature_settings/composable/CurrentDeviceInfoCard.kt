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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.models.LocalDeviceInfoModel
import com.sam.bluepad.presentation.composables.DeviceOSTypeContainer
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_rename
import com.sam.bluepad.resources.action_set_device_local_name
import com.sam.bluepad.resources.dialog_action_cancel
import com.sam.bluepad.resources.ic_edit_pen
import com.sam.bluepad.resources.settings_device_rename_dialog_text
import com.sam.bluepad.resources.settings_device_rename_dialog_title
import com.sam.bluepad.theme.Dimensions
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentDeviceInfoCard(
    deviceState: LocalDeviceInfoModel,
    onUpdateName: (String) -> Unit,
    modifier: Modifier = Modifier,
    devicePlatform: DevicePlatformOS = DevicePlatformOS.UNKNOWN,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {

    var showDialog by rememberSaveable { mutableStateOf(false) }

    UpdateDeviceNameDialog(
        showDialog = showDialog,
        initialName = deviceState.name,
        onDismissDialog = { showDialog = false },
        onConfirmName = { newName ->
            onUpdateName(newName)
            showDialog = false
        },
    )

    Card(
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColorFor(containerColor),
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(all = Dimensions.CARD_INTERNAL_PADDING_LARGE),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = deviceState.name,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                Text(
                    text = buildAnnotatedString {
                        append("ID: ")
                        withStyle(
                            style = SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) {
                            append(deviceState.deviceId.toHexString())
                        }
                    },
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                    onClick = { showDialog = true },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColorFor(containerColor),
                        contentColor = containerColor,
                    ),
                    contentPadding = ButtonDefaults.SmallContentPadding,
                ) {
                    Icon(painterResource(Res.drawable.ic_edit_pen), contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(Res.string.action_set_device_local_name),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                    )
                }
            }
            DeviceOSTypeContainer(
                deviceOs = devicePlatform,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(80.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDeviceNameDialog(
    showDialog: Boolean,
    onDismissDialog: () -> Unit,
    onConfirmName: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    containerColor: Color = AlertDialogDefaults.containerColor,
    contentColor: Color = AlertDialogDefaults.containerColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    shadowElevation: Dp = 0.dp,
    shape: Shape = AlertDialogDefaults.shape,
    properties: DialogProperties = DialogProperties()
) {
    val textFieldState = rememberTextFieldState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(textFieldState) {
        textFieldState.edit { insert(0, initialName) }
    }

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text }.onEach {
            if (errorMessage != null) {
                errorMessage = null
            }
        }.launchIn(this)
    }

    if (!showDialog) return

    BasicAlertDialog(
        onDismissRequest = onDismissDialog,
        properties = properties,
        modifier = modifier,
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            shape = shape,
        ) {
            Column(
                modifier = Modifier.padding(all = Dimensions.DIALOG_CONTENT_PADDING),
            ) {
                Text(
                    text = stringResource(Res.string.settings_device_rename_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text = stringResource(Res.string.settings_device_rename_dialog_text),
                    color = AlertDialogDefaults.textContentColor,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                OutlinedTextField(
                    state = textFieldState,
                    label = { Text(text = "New Name") },
                    shape = MaterialTheme.shapes.medium,
                    textStyle = MaterialTheme.typography.bodySmallEmphasized,
                    onKeyboardAction = {},
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = slideInVertically(MaterialTheme.motionScheme.slowEffectsSpec()),
                    exit = slideOutVertically(MaterialTheme.motionScheme.slowEffectsSpec()),
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TextButton(onClick = onDismissDialog) {
                        Text(
                            text = stringResource(Res.string.dialog_action_cancel),
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                        )
                    }
                    Button(
                        onClick = {
                            if (textFieldState.text.isNotEmpty()) {
                                onConfirmName(textFieldState.text.toString())
                                onDismissDialog()
                            } else {
                                errorMessage = "Empty name not allowed"
                            }
                        },
                        enabled = textFieldState.text.isNotEmpty(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = stringResource(Res.string.action_rename),
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                        )
                    }
                }
            }
        }
    }
}
