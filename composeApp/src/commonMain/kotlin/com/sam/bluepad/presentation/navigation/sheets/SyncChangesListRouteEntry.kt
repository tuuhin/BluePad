package com.sam.bluepad.presentation.navigation.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_sync.SyncChangesSheetContent
import com.sam.bluepad.presentation.feature_sync.viewmodel.SyncChangesViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.navigation.utils.BottomSheetSceneStrategy
import com.sam.bluepad.presentation.utils.UiEventsHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.syncChangesListRouteEntry(
    backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.SyncChangesListRouteEntry>(
    metadata = BottomSheetSceneStrategy.bottomSheet(
        ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false,
        ),
    ),
) {

    val viewModel = koinViewModel<SyncChangesViewModel> { parametersOf(it.session) }

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    UiEventsHandler(
        viewModel::uiEvent,
        onNavigateBack = { backStack.removeLastOrNull() },
    )

    SyncChangesSheetContent(
        state = screenState,
        onEvent = viewModel::onEvent,
    )
}
