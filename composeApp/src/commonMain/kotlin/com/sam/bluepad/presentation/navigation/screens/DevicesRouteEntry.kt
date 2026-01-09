package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_devices.screens.ManageDevicesScreen
import com.sam.bluepad.presentation.feature_devices.viewmodel.ManageDeviceViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.devicesRouteEntry(
	backStack: NavBackStack<NavKey>
) = entry<AssociatedNavGraph.DeviceRoute> {

	val viewModel = koinViewModel<ManageDeviceViewmodel>()
	val savedDevices by viewModel.devices.collectAsStateWithLifecycle()
	val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

	UiEventsHandler(viewModel::uiEvent)

	ManageDevicesScreen(
		devices = savedDevices,
		isLoading = isLoading,
		onEvent = viewModel::onEvent,
		onNavigateToAdvertise = { backStack.add(RootNavGraph.AdvertiseDeviceRoute) },
		onNavigateToAddDevice = { backStack.add(RootNavGraph.SearchDeviceRoute) }
	)
}