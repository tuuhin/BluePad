package com.sam.bluepad.presentation.navigation.dialogs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.sam.bluepad.presentation.feature_sync.SendSyncDialog
import com.sam.bluepad.presentation.feature_sync.viewmodel.SendDeviceSyncViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.syncDeviceRouteEntry(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.SendSyncDeviceRoute>(
	metadata = DialogSceneStrategy.dialog(),
) {

	val viewModel = koinViewModel<SendDeviceSyncViewModel>()

	SendSyncDialog(
		onEvent = viewModel::onEvent,
		onCancel = { backStack.removeLastOrNull() },
	)
}