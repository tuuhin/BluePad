package com.sam.bluepad.presentation.navigation

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

    val spatialEffect = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fastSpatialFloatEffect = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        sharedTransitionScope = LocalSharedTransitionScope.current,
        sceneStrategies = listOf(DialogSceneStrategy(), BottomSheetSceneStrategy(), SinglePaneSceneStrategy()),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            scaleIn(
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                initialScale = 0.9f,
            ) + fadeIn(animationSpec = spatialEffect) togetherWith
                scaleOut(
                    animationSpec = tween(durationMillis = 120, easing = EaseOutCubic),
                    targetScale = 1.1f,
                ) + fadeOut(fastSpatialFloatEffect)
        },
        popTransitionSpec = {
            scaleIn(
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                initialScale = 1.1f,
            ) + fadeIn(animationSpec = spatialEffect) togetherWith
                scaleOut(
                    animationSpec = tween(durationMillis = 120, easing = EaseOutCubic),
                    targetScale = 0.9f,
                ) + fadeOut(fastSpatialFloatEffect)
        },
        predictivePopTransitionSpec = {
            scaleIn(
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                initialScale = 1.1f,
            ) + fadeIn(animationSpec = spatialEffect) togetherWith
                scaleOut(
                    animationSpec = tween(durationMillis = 120, easing = EaseOutCubic),
                    targetScale = 0.9f,
                ) + fadeOut(fastSpatialFloatEffect)
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
