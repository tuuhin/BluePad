package com.sam.bluepad.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.navigation.nav_graph.AssociatedNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

object NavigationSerializers {

	fun rootNavGraphSerializer() = SerializersModule {
		polymorphic(NavKey::class) {
			subclass(
				RootNavGraph.AddOrUpdateRoute::class,
				RootNavGraph.AddOrUpdateRoute.serializer()
			)
			subclass(
				RootNavGraph.AssociatedNavGraphRoute::class,
				RootNavGraph.AssociatedNavGraphRoute.serializer()
			)
			subclass(
				RootNavGraph.AdvertiseDeviceRoute::class,
				RootNavGraph.AdvertiseDeviceRoute.serializer()
			)
			subclass(
				RootNavGraph.SearchDeviceRoute::class,
				RootNavGraph.SearchDeviceRoute.serializer()
			)
		}
	}

	fun associatedNavGraphSerializer() = SerializersModule {
		polymorphic(NavKey::class) {
			subclass(
				AssociatedNavGraph.ListRoute::class,
				AssociatedNavGraph.ListRoute.serializer()
			)
			subclass(
				AssociatedNavGraph.SettingsRoute::class,
				AssociatedNavGraph.SettingsRoute.serializer()
			)
			subclass(
				AssociatedNavGraph.DeviceRoute::class,
				AssociatedNavGraph.DeviceRoute.serializer()
			)
		}
	}
}