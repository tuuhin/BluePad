package com.sam.bluepad.presentation.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.feature_devices.ManageDevicesScreen
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph

fun EntryProviderScope<NavKey>.devicesRouteEntry(
	backStack: NavBackStack<NavKey>
) = entry<AssociatedNavGraph.DeviceRoute> {
	ManageDevicesScreen()
}