package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.ExternalDeviceModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SavedExternalDevicesList(
	devices: ImmutableList<ExternalDeviceModel>,
	onSyncDevice: (ExternalDeviceModel) -> Unit,
	onRevokeDevice: (ExternalDeviceModel) -> Unit,
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
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		contentPadding = contentPadding,
		modifier = modifier
	) {
		itemsIndexed(
			items = devices,
			key = listKey,
			contentType = contentType
		) { _, device ->
			ExternalDeviceCard(
				device = device,
				onSync = { onSyncDevice(device) },
				onRevoke = { onRevokeDevice(device) },
				modifier = Modifier.animateItem()
			)
		}
	}

}