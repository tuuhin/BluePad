package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.DeviceOSTypeContainer
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_delete
import com.sam.bluepad.resources.black_list_devices_screen_action_restore
import com.sam.bluepad.resources.device_device_id
import com.sam.bluepad.resources.ic_delete
import com.sam.bluepad.theme.Dimensions
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BlackListedDevicesList(
	devices: ImmutableList<ExternalDeviceModel>,
	onUnRevoke: (ExternalDeviceModel) -> Unit,
	onDeleteDevice: (ExternalDeviceModel) -> Unit,
	modifier: Modifier = Modifier,
	listState: LazyGridState = rememberLazyGridState(),
	contentPadding: PaddingValues = PaddingValues.Zero,
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val listKey: ((Int, ExternalDeviceModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.id }
	}

	val contentType: ((Int, ExternalDeviceModel) -> Any?) = remember {
		{ _, _ -> ExternalDeviceModel::class.simpleName }
	}

	LazyVerticalGrid(
		state = listState,
		columns = GridCells.Adaptive(minSize = 300.dp),
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		contentPadding = contentPadding,
		modifier = modifier
	) {
		itemsIndexed(
			items = devices,
			key = listKey,
			contentType = contentType
		) { _, device ->
			BlackListedDeviceItem(
				device = device,
				onUnRevoke = { onUnRevoke(device) },
				onDeleteDevice = { onDeleteDevice(device) },
				modifier = Modifier.animateItem()
			)
		}
	}
}

@Composable
private fun BlackListedDeviceItem(
	device: ExternalDeviceModel,
	onUnRevoke: () -> Unit,
	onDeleteDevice: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = MaterialTheme.shapes.extraLarge,
) {
	Card(
		modifier = modifier,
		shape = shape,
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier.fillMaxWidth()
				.padding(Dimensions.CARD_INTERNAL_PADDING),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				DeviceOSTypeContainer(
					deviceOs = device.deviceOs,
					containerColor = MaterialTheme.colorScheme.secondary
				)
				Column {
					Text(
						text = device.displayName ?: "N/A",
						style = MaterialTheme.typography.bodyLargeEmphasized,
						color = MaterialTheme.colorScheme.onSurface
					)
					Text(
						text = stringResource(Res.string.device_device_id, device.id.toHexString()),
						style = MaterialTheme.typography.bodyMediumEmphasized,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						fontFamily = FontFamily.Monospace,
						fontWeight = FontWeight.SemiBold,
					)
				}
			}
			Row(
				modifier = Modifier.align(Alignment.End),
				horizontalArrangement = Arrangement.spacedBy(4.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				FilledIconButton(
					onClick = onDeleteDevice,
					modifier = Modifier.size(
						IconButtonDefaults.smallContainerSize(
							IconButtonDefaults.IconButtonWidthOption.Wide
						)
					),
					shapes = IconButtonDefaults.shapes(
						shape = IconButtonDefaults.standardShape,
						pressedShape = IconButtonDefaults.mediumRoundShape
					),
					colors = IconButtonDefaults.filledIconButtonColors(
						containerColor = MaterialTheme.colorScheme.errorContainer,
						contentColor = MaterialTheme.colorScheme.onErrorContainer
					)
				) {
					Icon(
						painter = painterResource(Res.drawable.ic_delete),
						contentDescription = stringResource(Res.string.action_delete)
					)
				}
				Button(
					onClick = onUnRevoke,
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.tertiaryContainer,
						contentColor = MaterialTheme.colorScheme.onTertiaryContainer
					),
					shapes = ButtonDefaults.shapes(
						shape = ButtonDefaults.shape,
						pressedShape = ButtonDefaults.mediumPressedShape
					),
					contentPadding = ButtonDefaults.SmallContentPadding,
				) {
					Text(
						text = stringResource(Res.string.black_list_devices_screen_action_restore),
						style = MaterialTheme.typography.bodyMediumEmphasized
					)
				}
			}
		}
	}
}