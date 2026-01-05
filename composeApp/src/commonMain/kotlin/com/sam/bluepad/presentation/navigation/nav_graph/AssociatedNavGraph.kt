package com.sam.bluepad.presentation.navigation.nav_graph

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AssociatedNavGraph : NavKey {

	@Serializable
	data object ListRoute : AssociatedNavGraph

	@Serializable
	data object DeviceRoute : AssociatedNavGraph

	@Serializable
	data object SettingsRoute : AssociatedNavGraph
}