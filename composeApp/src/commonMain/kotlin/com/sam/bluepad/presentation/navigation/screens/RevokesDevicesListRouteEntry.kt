package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_devices.screens.BlackListedDevicesListScreen
import com.sam.bluepad.presentation.feature_devices.viewmodel.BlackListedDevicesViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.blackListedDevicesRoute(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.BlackListedDevicesRoute> {

	val viewModel = koinViewModel<BlackListedDevicesViewmodel>()
	val savedDevices by viewModel.devices.collectAsStateWithLifecycle()
	val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

	UiEventsHandler(viewModel::uiEvent)

	BlackListedDevicesListScreen(
		devices = savedDevices,
		isLoading = isLoading,
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
		}
	)

}