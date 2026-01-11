package com.sam.bluepad.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sam.bluepad.presentation.navigation.dialogs.advertiseDeviceEntry
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.navigation.screens.associatedNavGraphEntry
import com.sam.bluepad.presentation.navigation.screens.connectDeviceEntry
import com.sam.bluepad.presentation.navigation.screens.createOrUpdateSketchesEntry
import com.sam.bluepad.presentation.navigation.screens.searchDevicesEntry

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
		sceneStrategy = remember { DialogSceneStrategy() },
		entryDecorators = listOf(
			rememberSaveableStateHolderNavEntryDecorator(),
			rememberViewModelStoreNavEntryDecorator(),
		),
		entryProvider = entryProvider {
			associatedNavGraphEntry(backStack, startDestination = AssociatedNavGraph.ListRoute)
			createOrUpdateSketchesEntry(backStack)

			// devices route
			advertiseDeviceEntry(backStack)
			searchDevicesEntry(backStack)
			connectDeviceEntry(backStack)
		}
	)
}