package com.sam.bluepad.presentation.navigation.nav_graph

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
@Stable
sealed interface RootTabLayoutNavGraph : NavKey {

	@Serializable
    data object ListRoute : RootTabLayoutNavGraph

	@Serializable
    data object DeviceRoute : RootTabLayoutNavGraph

	@Serializable
    data object SettingsRoute : RootTabLayoutNavGraph
}
