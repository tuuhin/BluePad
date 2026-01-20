package com.sam.bluepad.presentation.feature_sync.composables

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.ExternalDeviceModel
import com.sam.bluepad.presentation.composables.ListContentLoadingWrapper
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_empty_box
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource

@Composable
fun ReceiveDevicesOptionsList(
	devicesList: ImmutableList<ExternalDeviceModel>,
	onSelectDevice: (ExternalDeviceModel) -> Unit,
	modifier: Modifier = Modifier,
	isAdvertising: Boolean = false,
	selectedDevice: ExternalDeviceModel? = null,
	paddingValues: PaddingValues = PaddingValues.Zero,
) {
	val isLocalInspectionMode = LocalInspectionMode.current

	val listKey: ((Int, ExternalDeviceModel) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.id }
	}

	val contentType: ((Int, ExternalDeviceModel) -> Any?) = remember {
		{ _, _ -> ExternalDeviceModel::class.simpleName }
	}

	ListContentLoadingWrapper(
		content = devicesList,
		isLoading = false,
		modifier = modifier,
		onItems = { items ->
			LazyVerticalGrid(
				columns = GridCells.Fixed(3),
				verticalArrangement = Arrangement.spacedBy(6.dp),
				horizontalArrangement = Arrangement.spacedBy(6.dp),
				contentPadding = paddingValues,
				modifier = Modifier.fillMaxSize()
			) {
				item(span = { GridItemSpan(maxCurrentLineSpan) }) {
					Text(
						text = "${devicesList.size} devices trying to sync",
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.surfaceVariant,
						modifier = Modifier.fillMaxWidth(),
						textAlign = TextAlign.Center,
					)
				}
				itemsIndexed(items = items, key = listKey, contentType = contentType) { _, item ->
					FoundDeviceCard(
						device = item,
						isSelected = item == selectedDevice,
						onSelectDevice = { onSelectDevice(item) },
						modifier = Modifier.animateItem()
					)
				}
			}
		},
		onEmpty = {
			Crossfade(
				targetState = isAdvertising,
				animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
				modifier = Modifier.padding(paddingValues)
			) { isActive ->
				if (isActive) DevicesSearchRunning(modifier = Modifier.fillMaxSize())
				else DevicesSearchEmptyResult(modifier = Modifier.fillMaxSize())
			}
		},
	)
}


@Composable
private fun DevicesSearchRunning(modifier: Modifier = Modifier) {
	Column(
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier
	) {
		LoadingIndicator()
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = "Looking for devices to sync in close proximity",
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onSurface
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = "Press sync from the other saved device",
			style = MaterialTheme.typography.labelMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Composable
private fun DevicesSearchEmptyResult(modifier: Modifier = Modifier) {
	Column(
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier
	) {
		Icon(
			painter = painterResource(Res.drawable.ic_empty_box),
			contentDescription = null
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = "Unable to find any device in close proximity",
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onSurface
		)
		Text(
			text = "Ensure sync started from the other saved device",
			style = MaterialTheme.typography.labelMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}