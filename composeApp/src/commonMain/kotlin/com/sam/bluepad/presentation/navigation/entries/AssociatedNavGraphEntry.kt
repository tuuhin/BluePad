package com.sam.bluepad.presentation.navigation.entries

import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sam.bluepad.presentation.navigation.NavigationSerializers
import com.sam.bluepad.presentation.navigation.composables.NavigationNavGraphWrapper
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.LocalSharedTransitionScope

fun EntryProviderScope<NavKey>.associatedNavGraphEntry(
	backStack: NavBackStack<NavKey>,
	startDestination: AssociatedNavGraph = AssociatedNavGraph.ListRoute,
) = entry<RootNavGraph.AssociatedNavGraphRoute> {

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
		onNavigateToAddRoute = { backStack.add(RootNavGraph.AddOrUpdateRoute()) },
	) {
		NavDisplay(
			backStack = nestedBackStack,
			onBack = { nestedBackStack.removeAll { it != startDestination } },
			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator(),
			),
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