package com.sam.bluepad.presentation.navigation.screens

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_settings.SettingsScreen
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph

fun EntryProviderScope<NavKey>.settingsRouteEntry(
	backStack: NavBackStack<NavKey>
) = entry<AssociatedNavGraph.SettingsRoute> {
	SettingsScreen()
}