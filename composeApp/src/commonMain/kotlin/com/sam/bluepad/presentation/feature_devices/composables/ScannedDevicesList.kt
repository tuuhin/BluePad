package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.ble.models.BLEPeerDevice
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ScanDeviceList(
	searchedPeers: ImmutableList<BLEPeerDevice>,
	onConnect: (BLEPeerDevice) -> Unit,
	onListRefresh: () -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues.Zero,
	isConnectEnabled: Boolean = true,
	isListRefreshing: Boolean = false,
	listState: LazyGridState = rememberLazyGridState()
) {
	val state = rememberPullToRefreshState()
	val isLocalInspectionMode = LocalInspectionMode.current

	val listKey: ((Int, BLEPeerDevice) -> Any)? = remember {
		if (isLocalInspectionMode) null
		else { _, device -> device.deviceAddress }
	}

	val contentType: ((Int, BLEPeerDevice) -> Any?) = remember {
		{ _, _ -> BLEPeerDevice::class.simpleName }
	}

	PullToRefreshBox(
		state = state,
		onRefresh = onListRefresh,
		isRefreshing = isListRefreshing,
		indicator = {
			Indicator(
				state = state,
				isRefreshing = isListRefreshing,
				containerColor = MaterialTheme.colorScheme.primaryContainer,
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				modifier = Modifier.align(Alignment.TopCenter),
			)
		}
	) {
		LazyVerticalGrid(
			state = listState,
			columns = GridCells.Adaptive(minSize = 200.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
			contentPadding = contentPadding,
			modifier = modifier
		) {
			itemsIndexed(
				items = searchedPeers,
				key = listKey,
				contentType = contentType
			) { _, device ->
				ScanDeviceCard(
					device = device,
					onConnect = { onConnect(device) },
					isActionEnabled = isConnectEnabled,
					modifier = Modifier.animateItem()
				)
			}
		}
	}
}