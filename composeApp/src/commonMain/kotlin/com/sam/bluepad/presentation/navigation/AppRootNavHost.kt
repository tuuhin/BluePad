package com.sam.bluepad.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sam.bluepad.presentation.navigation.entries.associatedNavGraphEntry
import com.sam.bluepad.presentation.navigation.entries.createOrUpdateSketchesEntry
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.utils.LocalSharedTransitionScope

@Composable
fun AppRootNavHost(modifier: Modifier = Modifier) {

	val backStack = rememberNavBackStack(
		configuration = SavedStateConfiguration {
			serializersModule = NavigationSerializers.rootNavGraphSerializer()
		},
		RootNavGraph.AssociatedNavGraphRoute,
	)

	NavDisplay(
		backStack = backStack,
		modifier = modifier,
		entryDecorators = listOf(
			rememberSaveableStateHolderNavEntryDecorator(),
			rememberViewModelStoreNavEntryDecorator(),
		),
		sharedTransitionScope = LocalSharedTransitionScope.current,
		entryProvider = entryProvider {
			associatedNavGraphEntry(backStack)
			createOrUpdateSketchesEntry(backStack)
		}
	)
}
