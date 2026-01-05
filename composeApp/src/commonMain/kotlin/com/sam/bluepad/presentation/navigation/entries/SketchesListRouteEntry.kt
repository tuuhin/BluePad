package com.sam.bluepad.presentation.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_list.SketchesListScreen
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph

fun EntryProviderScope<NavKey>.sketchesListRouteEntry(
	backStack: NavBackStack<NavKey>
) = entry<AssociatedNavGraph.ListRoute> {
	SketchesListScreen()
}