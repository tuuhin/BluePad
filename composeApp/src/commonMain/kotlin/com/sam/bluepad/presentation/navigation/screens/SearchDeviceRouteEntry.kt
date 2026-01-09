package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_devices.screens.AddDevicesScreen
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEScanDevicesViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.searchDevicesEntry(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.SearchDeviceRoute> {

	val viewModel = koinViewModel<BLEScanDevicesViewModel>()

	val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
	val isListRefreshing by viewModel.isListRefreshing.collectAsStateWithLifecycle()
	val peers by viewModel.scanPeers.collectAsStateWithLifecycle()

	UiEventsHandler(viewModel::uiEvent)

	AddDevicesScreen(
		searchedPeers = peers,
		isScanRunning = isScanning,
		isListRefreshing = isListRefreshing,
		onEvent = viewModel::onEvent,
		onNavigateToConnect = { address -> backStack.add(RootNavGraph.ConnectDeviceRoute(address)) },
		onBackNavigation = {
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