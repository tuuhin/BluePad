package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.DeviceOSTypeContainer
import com.sam.bluepad.presentation.utils.PreviewFakes
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_blacklist_device
import com.sam.bluepad.resources.action_delete_device
import com.sam.bluepad.resources.action_sync_device
import com.sam.bluepad.resources.ic_delete
import com.sam.bluepad.resources.ic_unlink
import com.sam.bluepad.resources.ic_vert_menu
import com.sam.bluepad.resources.scan_result_device_name_title
import com.sam.bluepad.theme.BluePadTheme
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun ExternalDeviceCard(
	device: ExternalDeviceModel,
	onSync: () -> Unit,
	onRevoke: () -> Unit,
	onDelete: () -> Unit,
	modifier: Modifier = Modifier,
	isActionEnabled: Boolean = true,
	shape: Shape = MaterialTheme.shapes.large,
) {
	var showDropdown by remember { mutableStateOf(false) }

	Card(
		modifier = modifier.heightIn(min = Dimensions.EXTERNAL_DEVICE_CARD_MIN_HEIGHT),
		shape = shape,
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(all = Dimensions.CARD_INTERNAL_PADDING),
			verticalArrangement = Arrangement.spacedBy(4.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			DeviceOSTypeContainer(
				deviceOs = device.deviceOs,
				containerColor = MaterialTheme.colorScheme.tertiaryContainer,
				modifier = Modifier.size(64.dp)
			)
			Text(
				text = stringResource(Res.string.scan_result_device_name_title),
				style = MaterialTheme.typography.labelMediumEmphasized,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Text(
				text = device.displayName ?: "N/A",
				style = MaterialTheme.typography.titleLargeEmphasized,
				color = MaterialTheme.colorScheme.primary,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				fontWeight = FontWeight.SemiBold,
			)
			Spacer(modifier = Modifier.height(2.dp))

			SplitButtonLayout(
				leadingButton = {
					SplitButtonDefaults.LeadingButton(
						onClick = onSync,
						colors = ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.primaryContainer,
							contentColor = MaterialTheme.colorScheme.onPrimaryContainer
						),
						enabled = isActionEnabled,
					) {
						Text(
							text = stringResource(Res.string.action_sync_device),
							style = MaterialTheme.typography.titleMedium
						)
					}
				},
				trailingButton = {
					Box {
						SplitButtonDefaults.TrailingButton(
							checked = showDropdown,
							onCheckedChange = { showDropdown = true },
						) {
							Icon(
								painter = painterResource(Res.drawable.ic_vert_menu),
								contentDescription = null,
								modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize)
							)
						}
						DropdownMenu(
							expanded = showDropdown,
							onDismissRequest = { showDropdown = false },
							shape = MaterialTheme.shapes.large
						) {
							DropdownMenuItem(
								text = { Text(text = stringResource(Res.string.action_blacklist_device)) },
								leadingIcon = {
									Icon(
										painter = painterResource(Res.drawable.ic_unlink),
										contentDescription = null
									)
								},
								onClick = onRevoke,
							)
							DropdownMenuItem(
								text = { Text(text = stringResource(Res.string.action_delete_device)) },
								leadingIcon = {
									Icon(
										painter = painterResource(Res.drawable.ic_delete),
										contentDescription = null
									)
								},
								onClick = onDelete,
								colors = MenuDefaults.itemColors(
									textColor = MaterialTheme.colorScheme.error,
									leadingIconColor = MaterialTheme.colorScheme.error
								)
							)
						}
					}
				},
			)
		}
	}
}

@Preview
@Composable
private fun ExternalDeviceCardPreview() = BluePadTheme {
	ExternalDeviceCard(
		device = PreviewFakes.FAKE_EXTERNAL_MODEL,
		onSync = {},
		onRevoke = {},
		onDelete = {},
	)
}