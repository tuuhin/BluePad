package com.sam.bluepad.presentation.navigation.dialogs

import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.sam.bluepad.presentation.feature_devices.screens.ConnectDeviceDialogContent
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEConnectDeviceViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.connectDeviceEntry(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.ConnectDeviceRoute>(
	metadata = DialogSceneStrategy.dialog(dialogProperties = DialogProperties())
) { route ->

	val viewModel = koinViewModel<BLEConnectDeviceViewmodel>(
		parameters = { parametersOf(route.address) },
	)

	val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
	val peerDataFound by viewModel.connectedPeerData.collectAsStateWithLifecycle()
	val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle(null)

	UiEventsHandler(
		eventsFlow = viewModel::uiEvent,
		onNavigateBack = { backStack.removeLastOrNull() },
	)

	ConnectDeviceDialogContent(
		connectionState = connectionState,
		connectedPeerData = peerDataFound,
		errorMessage = errorMessage,
		onEvent = viewModel::onEvent,
	)
}