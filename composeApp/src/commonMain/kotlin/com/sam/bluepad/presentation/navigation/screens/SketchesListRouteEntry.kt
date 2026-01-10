package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_sketches.screens.SketchesListScreen
import com.sam.bluepad.presentation.feature_sketches.viewmodel.SketchesViewmodel
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.sketchesListRouteEntry(
	backStack: NavBackStack<NavKey>
) = entry<AssociatedNavGraph.ListRoute> {

	val viewModel = koinViewModel<SketchesViewmodel>()
	val sketches by viewModel.sketches.collectAsStateWithLifecycle()
	val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
	val isSketchSelected by viewModel.isSketchSelected.collectAsStateWithLifecycle()

	UiEventsHandler(
		eventsFlow = viewModel::uiEvent,
		onNavigateBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
	)

	SketchesListScreen(
		sketches = sketches,
		isLoading = isLoading,
		onEvent = viewModel::onEvent,
		showDeleteDialog = isSketchSelected,
		onNavigateToNewSketch = { backStack.add(RootNavGraph.AddOrUpdateRoute()) },
		onNavigateToSketch = { sketch ->
			backStack.add(RootNavGraph.AddOrUpdateRoute(sketch.id))
		},
	)
}