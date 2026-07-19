package com.sam.bluepad.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sam.bluepad.presentation.navigation.dialogs.advertiseDeviceEntry
import com.sam.bluepad.presentation.navigation.dialogs.connectDeviceEntry
import com.sam.bluepad.presentation.navigation.dialogs.createBondRouteEntry
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootTabLayoutNavGraph
import com.sam.bluepad.presentation.navigation.scene.BottomSheetSceneStrategy
import com.sam.bluepad.presentation.navigation.screens.blackListedDevicesRoute
import com.sam.bluepad.presentation.navigation.screens.createOrUpdateSketchesEntry
import com.sam.bluepad.presentation.navigation.screens.receiveSyncDataRouteEntry
import com.sam.bluepad.presentation.navigation.screens.rootTabLayoutEntry
import com.sam.bluepad.presentation.navigation.screens.searchDevicesEntry
import com.sam.bluepad.presentation.navigation.screens.syncDeviceRouteEntry
import com.sam.bluepad.presentation.navigation.sheets.syncChangesListRouteEntry
import com.sam.bluepad.presentation.utils.LocalSharedTransitionScope

@Composable
fun AppRootNavHost(modifier: Modifier = Modifier) {

    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = NavigationSerializers.rootNavGraphSerializer()
        },
        RootNavGraph.TabLayoutRoute,
    )

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        sharedTransitionScope = LocalSharedTransitionScope.current,
        sceneStrategies = listOf(DialogSceneStrategy(), BottomSheetSceneStrategy(), SinglePaneSceneStrategy()),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            slideInVertically(initialOffsetY = { it }) + fadeIn() togetherWith
                slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        },
        popTransitionSpec = {
            slideInVertically(initialOffsetY = { -it }) + fadeIn() togetherWith
                slideOutVertically(targetOffsetY = { it }) + fadeOut()
        },
        predictivePopTransitionSpec = {
            slideInVertically(initialOffsetY = { -it }) + fadeIn() togetherWith
                slideOutVertically(targetOffsetY = { it }) + fadeOut()
        },
        entryProvider = entryProvider {
            rootTabLayoutEntry(backStack, startDestination = RootTabLayoutNavGraph.ListRoute)
            createOrUpdateSketchesEntry(backStack)

            // devices route
            advertiseDeviceEntry(backStack)
            searchDevicesEntry(backStack)
            connectDeviceEntry(backStack)
            blackListedDevicesRoute(backStack)
            createBondRouteEntry(backStack)

            // sync routes
            receiveSyncDataRouteEntry(backStack)
            syncDeviceRouteEntry(backStack)
            syncChangesListRouteEntry(backStack)
        },
    )
}
