package com.sam.bluepad.presentation.navigation.dialogs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.sam.bluepad.presentation.feature_bond.CreateBondDialogContent
import com.sam.bluepad.presentation.feature_bond.CreateDeviceBondViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.createBondRouteEntry(
    backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.CreateDeviceBondRoute>(
    metadata = DialogSceneStrategy.dialog(
        dialogProperties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ),
) { route ->

    val viewModel = koinViewModel<CreateDeviceBondViewmodel>(
        parameters = { parametersOf(route.identifier) },
    )

    val state by viewModel.bondDialogState.collectAsStateWithLifecycle()

    UiEventsHandler(
        eventsFlow = viewModel::uiEvent,
        onNavigateBack = { backStack.removeLastOrNull() },
    )

    val simpleName = remember(route) {
        if (route.localName == null || route.localName == "unknown" || route.localName == "unnamed") route.identifier
        else route.localName
    }

    CreateBondDialogContent(
        state = state,
        identifier = simpleName,
        onEvent = viewModel::onEvent,
    )
}
