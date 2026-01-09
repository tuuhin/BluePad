package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_devices.screens.ConnectDeviceScreen
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEConnectDeviceViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.connectDeviceEntry(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.ConnectDeviceRoute>(
) { route ->

	val viewModel = koinViewModel<BLEConnectDeviceViewmodel>(
		parameters = { parametersOf(route.address) },
	)

	val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
	val peerDataFound by viewModel.peerData.collectAsStateWithLifecycle()

	UiEventsHandler(
		eventsFlow = viewModel::uiEvent,
		onNavigateBack = {
			if (backStack.size > 1) backStack.removeLastOrNull()
		},
	)

	ConnectDeviceScreen(
		connectionState = connectionState,
		hasPeerDataFound = peerDataFound,
		onEvent = viewModel::onEvent,
		onNavigateBack = {
			if (backStack.isNotEmpty()) {
				IconButton(onClick = { backStack.removeLastOrNull() }) {
					Icon(
						painter = painterResource(Res.drawable.ic_back),
						contentDescription = stringResource(Res.string.action_back)
					)
				}
			}
		}
	)
}