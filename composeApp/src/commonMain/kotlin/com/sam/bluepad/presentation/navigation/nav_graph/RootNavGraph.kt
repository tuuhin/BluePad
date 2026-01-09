package com.sam.bluepad.presentation.navigation.nav_graph

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface RootNavGraph : NavKey {

	@Serializable
	data class AddOrUpdateRoute(val sketchId: Long? = null) : RootNavGraph

	@Serializable
	data object AssociatedNavGraphRoute : RootNavGraph

	@Serializable
	data object AdvertiseDeviceRoute : RootNavGraph

	@Serializable
	data object SearchDeviceRoute : RootNavGraph

	@Serializable
	data class ConnectDeviceRoute(val address: String) : RootNavGraph
}