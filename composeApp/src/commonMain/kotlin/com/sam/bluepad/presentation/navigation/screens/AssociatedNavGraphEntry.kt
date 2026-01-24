package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.presentation.navigation.NavigationSerializers
import com.sam.bluepad.presentation.navigation.composables.NavigationNavGraphWrapper
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.LocalSharedTransitionScope
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo

fun EntryProviderScope<NavKey>.associatedNavGraphEntry(
	backStack: NavBackStack<NavKey>,
	startDestination: AssociatedNavGraph = AssociatedNavGraph.ListRoute,
) = entry<RootNavGraph.AssociatedNavGraphRoute> {

	val windowSize = LocalWindowSizeInfo.current

	val nestedBackStack = rememberNavBackStack(
		configuration = SavedStateConfiguration {
			serializersModule = NavigationSerializers.associatedNavGraphSerializer()
		},
		startDestination,
	)

	// nested nav graph
	NavigationNavGraphWrapper(
		onSelectRoute = { nestedBackStack.add(it) },
		selectedRoute = nestedBackStack.filterIsInstance<AssociatedNavGraph>().lastOrNull(),
	) {
		NavDisplay(
			backStack = nestedBackStack,
			onBack = { nestedBackStack.removeAll { it != startDestination } },
			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator(),
			),
			transitionSpec = {
				if (windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND))
					fadeIn(
						initialAlpha = .4f,
						animationSpec = tween(durationMillis = 200, easing = EaseInExpo)
					) togetherWith fadeOut(
						animationSpec = tween(durationMillis = 100, easing = EaseOut)
					)
				else slideInHorizontally { width -> width / 2 } + fadeIn() togetherWith
						slideOutHorizontally { -it / 2 } + fadeOut()

			},
			predictivePopTransitionSpec = {
				if (windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND))
					fadeIn(
						initialAlpha = .4f,
						animationSpec = tween(durationMillis = 200, easing = EaseInExpo)
					) togetherWith fadeOut(
						animationSpec = tween(durationMillis = 100, easing = EaseOut)
					)
				else slideInHorizontally { width -> width / 2 } + fadeIn() togetherWith
						slideOutHorizontally { -it / 2 } + fadeOut()
			},
			sharedTransitionScope = LocalSharedTransitionScope.current,
			entryProvider = entryProvider {
				sketchesListRouteEntry(backStack)
				createOrUpdateSketchesEntry(backStack)
				devicesRouteEntry(backStack)
				settingsRouteEntry(backStack)
			}
		)
	}
}