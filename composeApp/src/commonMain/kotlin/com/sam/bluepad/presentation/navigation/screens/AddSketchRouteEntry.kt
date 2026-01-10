package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_sketches.screens.CreateSketchScreen
import com.sam.bluepad.presentation.feature_sketches.viewmodel.AddSketchViewModel
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.UiEventsHandler
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.action_back
import com.sam.bluepad.resources.ic_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.createOrUpdateSketchesEntry(
	backStack: NavBackStack<NavKey>
) = entry<RootNavGraph.AddOrUpdateRoute> { route ->

	val viewModel = koinViewModel<AddSketchViewModel>(
		parameters = { parametersOf(route.sketchId) }
	)

	val createState by viewModel.screenState.collectAsStateWithLifecycle()
	val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
	val isContentLoadFailed by viewModel.isContentLoadFailed.collectAsStateWithLifecycle()

	UiEventsHandler(
		eventsFlow = viewModel::uiEvent,
		onNavigateBack = { if (backStack.isNotEmpty()) backStack.removeLastOrNull() },
	)

	CreateSketchScreen(
		state = createState,
		onEvent = viewModel::onEvent,
		isLoading = isLoading,
		isContentLoadFailed = isContentLoadFailed,
		navigation = {
			if (backStack.isNotEmpty()) {
				IconButton(onClick = { backStack.removeLastOrNull() }) {
					Icon(
						painter = painterResource(Res.drawable.ic_back),
						contentDescription = stringResource(Res.string.action_back)
					)
				}
			}
		},
	)
}