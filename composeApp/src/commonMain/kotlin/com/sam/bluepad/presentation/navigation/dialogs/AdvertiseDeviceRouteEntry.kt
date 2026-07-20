package com.sam.bluepad.presentation.navigation.dialogs

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sam.bluepad.presentation.feature_devices.screens.DeviceAdvertisementDialog
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEAdvertisementViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.LocalAnimatedContentScope
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.advertiseDeviceEntry(
    backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.AdvertiseDeviceRoute>(
    metadata = DialogSceneStrategy.dialog(
        DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ),
) {

    val contentScope = LocalNavAnimatedContentScope.current

    val viewModel = koinViewModel<BLEAdvertisementViewmodel>()
    val isRunning by viewModel.isAdvertisementRunning.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalAnimatedContentScope provides contentScope) {
        DeviceAdvertisementDialog(
            isAdvertising = isRunning,
            errorMessage = errorMessage,
            onEvent = viewModel::onEvent,
            onCancel = { backStack.removeLastOrNull() },
        )
    }
}
