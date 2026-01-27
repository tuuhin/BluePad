package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_sync.ReceiveSyncDeviceListScreen
import com.sam.bluepad.presentation.feature_sync.viewmodel.ReceiveDeviceSyncViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.receiveSyncDataRouteEntry(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.ReceiveSyncDeviceRoute> {

	val viewModel = koinViewModel<ReceiveDeviceSyncViewModel>()

	val isAdvertising by viewModel.isAdvertising.collectAsStateWithLifecycle()
	val devices by viewModel.savedDevices.collectAsStateWithLifecycle()
	val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()

	ReceiveSyncDeviceListScreen(
		devicesList = devices,
		isAdvertising = isAdvertising,
		selectedDevice = selectedDevice,
		onEvent = viewModel::onEvent,
		navigation = {
			if (backStack.isNotEmpty()) {
				IconButton(onClick = { backStack.removeLastOrNull() }) {
					Icon(
						painter = painterResource(Res.drawable.ic_back),
						contentDescription = stringResource(Res.string.action_back)
					)
				}
			}
		},
	)
}