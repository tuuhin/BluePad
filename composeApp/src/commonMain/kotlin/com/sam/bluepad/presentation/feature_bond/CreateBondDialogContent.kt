package com.sam.bluepad.presentation.feature_bond

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.bluepad.presentation.feature_bond.state.CreateBondDialogEvents
import com.sam.bluepad.presentation.feature_bond.state.CreateBondDialogState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.create_bond_dialog_text
import com.sam.bluepad.resources.create_bond_dialog_title
import com.sam.bluepad.resources.dialog_action_create_bond
import com.sam.bluepad.resources.dialog_action_deny
import com.sam.bluepad.resources.dialog_action_request
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateBondDialogContent(
    state: CreateBondDialogState,
    onEvent: (CreateBondDialogEvents) -> Unit,
    modifier: Modifier = Modifier,
    identifier: String? = null,
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
        modifier = modifier.widthIn(min = Dimensions.DIALOG_MIN_WIDTH)
            .width(IntrinsicSize.Max),
    ) {
        Column(
            modifier = Modifier.padding(all = Dimensions.DIALOG_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.create_bond_dialog_title, identifier ?: "to device"),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = titleContentColor,
            )

            Spacer(modifier = Modifier.height(Dimensions.DIALOG_SECTIONS_PADDING))
            Text(
                text = stringResource(Res.string.create_bond_dialog_text),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = textContentColor,
                textAlign = TextAlign.Center,
            )

            AnimatedVisibility(
                visible = state.canShowConfirmPinInDialog && state.confirmPin != null,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = state.confirmPin ?: "",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                state.error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimensions.DIALOG_SECTIONS_PADDING))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onEvent(CreateBondDialogEvents.OnCancelBondForDevice) }) {
                    Text(text = stringResource(Res.string.dialog_action_deny))
                }
                Spacer(modifier = Modifier.width(Dimensions.DIALOG_ACTIONS_SPACING))
                Button(
                    onClick = {
                        if (state.confirmPin != null) onEvent(CreateBondDialogEvents.OnAcceptConfirmPin)
                        else onEvent(CreateBondDialogEvents.OnRequestBondForDevice)
                    },
                    shapes = ButtonDefaults.shapes(
                        shape = ButtonDefaults.elevatedShape,
                        pressedShape = ButtonDefaults.mediumPressedShape,
                    ),
                    enabled = state.isPrimaryActionEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    AnimatedVisibility(
                        visible = !state.isPrimaryActionEnabled,
                        enter = scaleIn(),
                        exit = scaleOut(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    if (state.confirmPin != null) Text(text = stringResource(Res.string.dialog_action_create_bond))
                    else Text(text = stringResource(Res.string.dialog_action_request))
                }
            }
        }
    }
}
