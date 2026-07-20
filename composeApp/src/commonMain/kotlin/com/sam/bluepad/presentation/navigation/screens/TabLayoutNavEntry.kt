package com.sam.bluepad.presentation.navigation.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.TransformOrigin
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sam.bluepad.presentation.navigation.NavigationSerializers
import com.sam.bluepad.presentation.navigation.composables.AdaptiveNavigationSuitWrapper
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootTabLayoutNavGraph
import com.sam.bluepad.presentation.utils.LocalAnimatedContentScope

fun EntryProviderScope<NavKey>.rootTabLayoutEntry(
    backStack: NavBackStack<NavKey>,
    startDestination: RootTabLayoutNavGraph = RootTabLayoutNavGraph.ListRoute,
) = entry<RootNavGraph.TabLayoutRoute> {

    val contentScope = LocalNavAnimatedContentScope.current

    val fastEffect = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val defaultEffect = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    val nestedBackStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = NavigationSerializers.rootTabLayoutSerializer()
        },
        startDestination,
    )

    // nested nav graph
    CompositionLocalProvider(LocalAnimatedContentScope provides contentScope) {
        AdaptiveNavigationSuitWrapper(
            onSelectRoute = { nestedBackStack.add(it) },
            selectedRoute = nestedBackStack.filterIsInstance<RootTabLayoutNavGraph>().lastOrNull(),
        ) {
            NavDisplay(
                backStack = nestedBackStack,
                onBack = { nestedBackStack.removeAll { it != startDestination } },
                sharedTransitionScope = null,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = {
                    fadeIn(
                        initialAlpha = .8f,
                        animationSpec = defaultEffect,
                    ) + scaleIn(
                        animationSpec = defaultEffect,
                        transformOrigin = TransformOrigin.Center,
                        initialScale = .9f,
                    ) togetherWith fadeOut(targetAlpha = .1f, animationSpec = fastEffect)
                },
                predictivePopTransitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider {
                    sketchesListRouteEntry(backStack)
                    devicesRouteEntry(backStack)
                    settingsRouteEntry(backStack)
                },
            )
        }
    }
}
