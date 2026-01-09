package com.sam.bluepad.presentation.feature_devices.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.bluepad.presentation.feature_devices.events.AdvertisementScreenEvent
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.device_advertisement_action_start
import com.sam.bluepad.resources.device_advertisement_action_stop
import com.sam.bluepad.resources.device_advertisement_dialog_points
import com.sam.bluepad.resources.device_advertisement_dialog_text
import com.sam.bluepad.resources.device_advertisement_dialog_title
import com.sam.bluepad.resources.dialog_action_cancel
import com.sam.bluepad.theme.Dimensions
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DeviceAdvertisementDialog(
	isAdvertising: Boolean,
	onEvent: (AdvertisementScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
	errorMessage: String? = null,
	onCancel: () -> Unit = {},
	shape: Shape = AlertDialogDefaults.shape,
	containerColor: Color = AlertDialogDefaults.containerColor,
	tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
	titleContentColor: Color = AlertDialogDefaults.titleContentColor,
	textContentColor: Color = AlertDialogDefaults.textContentColor,
	shadowElevation: Dp = 0.dp,
) {

	val permissionController = koinInject<PermissionsController>()
	val scope = rememberCoroutineScope()

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
				text = stringResource(Res.string.device_advertisement_dialog_title),
				style = MaterialTheme.typography.headlineSmallEmphasized,
				color = titleContentColor
			)

			Spacer(modifier = Modifier.height(16.dp))
			Text(
				text = stringResource(Res.string.device_advertisement_dialog_text),
				style = MaterialTheme.typography.bodyMedium,
				color = textContentColor,
			)

			Column(
				verticalArrangement = Arrangement.spacedBy(10.dp),
				modifier = Modifier.padding(vertical = 20.dp)
			) {
				val points = stringArrayResource(Res.array.device_advertisement_dialog_points)
				points.forEach {
					BulletItem(it)
				}
			}

			AnimatedVisibility(
				visible = errorMessage != null,
				enter = slideInVertically() + fadeIn(),
				exit = slideOutVertically() + fadeOut(),
				modifier = Modifier.align(Alignment.CenterHorizontally)
			) {
				errorMessage?.let { message ->
					Text(
						text = message,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.error,
						modifier = Modifier.padding(vertical = 4.dp)
					)
				}
			}

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				TextButton(
					onClick = {
						// stop it and then cancel
						if (isAdvertising) onEvent(AdvertisementScreenEvent.OnStopAdvertise)
						onCancel()
					},
					enabled = !isAdvertising
				) {
					Text(text = stringResource(Res.string.dialog_action_cancel))
				}
				Spacer(modifier = Modifier.width(8.dp))
				Button(
					onClick = {
						scope.launch {
							when (permissionController.getPermissionState(Permission.BLUETOOTH_LE)) {
								PermissionState.NotGranted, PermissionState.NotDetermined, PermissionState.Denied -> {
									permissionController.providePermission(Permission.BLUETOOTH_LE)
								}

								PermissionState.Granted -> {
									if (isAdvertising) onEvent(AdvertisementScreenEvent.OnStopAdvertise)
									else onEvent(AdvertisementScreenEvent.OnStartAdvertise)
								}

								PermissionState.DeniedAlways -> permissionController.openAppSettings()
							}
						}
					},
					shapes = ButtonDefaults.shapes(
						shape = ButtonDefaults.elevatedShape,
						pressedShape = ButtonDefaults.mediumPressedShape
					),
					colors = ButtonDefaults.buttonColors(
						containerColor = if (isAdvertising) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
						contentColor = if (isAdvertising) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
					)
				) {
					if (isAdvertising) Text(text = stringResource(Res.string.device_advertisement_action_stop))
					else Text(text = stringResource(Res.string.device_advertisement_action_start))
				}
			}
		}
	}
}

@Composable
private fun BulletItem(text: String) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(12.dp)
	) {
		Box(
			modifier = Modifier
				.size(12.dp)
				.background(
					MaterialTheme.colorScheme.primary,
					MaterialShapes.Cookie4Sided.toShape()
				)
		)
		Text(
			text = text,
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}