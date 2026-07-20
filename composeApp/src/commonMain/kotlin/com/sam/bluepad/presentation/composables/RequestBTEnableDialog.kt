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
import com.sam.bluepad.resources.action_enable
import com.sam.bluepad.resources.bluetooth_not_enabled_dialog_text
import com.sam.bluepad.resources.bluetooth_not_enabled_dialog_title
import com.sam.bluepad.resources.ic_bt_not_enabled
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RequestBTEnableDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
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
            Button(
                onClick = onAccept,
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
                    text = stringResource(Res.string.action_enable),
                    fontWeight = FontWeight.SemiBold,
                )
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
                text = stringResource(Res.string.bluetooth_not_enabled_dialog_text),
                textAlign = TextAlign.Center,
            )
        },
    )
}
