package com.sam.bluepad.presentation.navigation.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_sync.ReviewSyncChangesSheetContent
import com.sam.bluepad.presentation.feature_sync.event.SyncWorkflowEvent
import com.sam.bluepad.presentation.feature_sync.viewmodel.ReviewSyncChangesViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.navigation.utils.BottomSheetSceneStrategy
import com.sam.bluepad.presentation.utils.UiEventsHandler
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.syncChangesListRouteEntry(
    backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.SyncChangesListRouteEntry>(
    metadata = BottomSheetSceneStrategy.bottomSheet(
        isSkipPartiallyExpanded = true,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false, shouldDismissOnClickOutside = false),
    ),
) { route ->

    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel = koinViewModel<ReviewSyncChangesViewModel> { parametersOf(route.sessionId, route.remoteDeviceId) }

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.workFlowEvent.collectLatest { event ->
                when (event) {
                    SyncWorkflowEvent.ReviewedAndSaved -> {
                        // Check this once
                        val idx = backStack.indexOf(RootNavGraph.AssociatedNavGraphRoute)
                        if (idx == -1) return@collectLatest

                        val oldStack = backStack.take(idx + 1)
                        backStack.clear()
                        backStack.addAll(oldStack)
                    }

                    else -> {}
                }
            }
        }
    }

    UiEventsHandler(
        eventsFlow = viewModel::uiEvent,
        onNavigateBack = { backStack.removeLastOrNull() },
    )

    ReviewSyncChangesSheetContent(
        state = screenState,
        onEvent = viewModel::onEvent,
    )
}
