package com.sam.bluepad.presentation.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RequestBTEnableDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onActivate: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    canRequestActivate: Boolean = false,
    canOpenSettings: Boolean = false,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
) {

    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        confirmButton = {
            if (canRequestActivate || canOpenSettings) {
                Button(
                    onClick = if (canRequestActivate) onActivate else onOpenSettings,
                    shapes = ButtonDefaults.shapes(
                        shape = ButtonDefaults.shape,
                        pressedShape = ButtonDefaults.pressedShape,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    contentPadding = ButtonDefaults.SmallContentPadding,
                ) {
                    Text(
                        text = if (canRequestActivate) stringResource(Res.string.action_enable)
                        else stringResource(Res.string.action_open_bluetooth_settings),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_bt_not_enabled),
                contentDescription = "Bluetooth not enabled",
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.bluetooth_not_enabled_dialog_title),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = if (canRequestActivate)
                    stringResource(Res.string.bluetooth_not_enabled_dialog_text_normal)
                else stringResource(Res.string.bluetooth_not_enabled_dialog_text_settings,),
                textAlign = TextAlign.Center,
            )
        },
    )
}
