package com.sam.bluepad.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.sam.bluepad.presentation.navigation.nav_graph.RootNavGraph
import com.sam.bluepad.presentation.navigation.nav_graph.RootTabLayoutNavGraph
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

object NavigationSerializers {

    fun rootNavGraphSerializer() = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(
                RootNavGraph.AddOrUpdateRoute::class,
                RootNavGraph.AddOrUpdateRoute.serializer(),
            )
            subclass(
                RootNavGraph.TabLayoutRoute::class,
                RootNavGraph.TabLayoutRoute.serializer(),
            )
            subclass(
                RootNavGraph.AdvertiseDeviceRoute::class,
                RootNavGraph.AdvertiseDeviceRoute.serializer(),
            )
            subclass(
                RootNavGraph.ConnectDeviceRoute::class,
                RootNavGraph.ConnectDeviceRoute.serializer(),
            )
            subclass(
                RootNavGraph.SearchDeviceRoute::class,
                RootNavGraph.SearchDeviceRoute.serializer(),
            )
            subclass(
                RootNavGraph.SyncConnectorRoute::class,
                RootNavGraph.SyncConnectorRoute.serializer(),
            )
            subclass(
                RootNavGraph.ReceiveSyncDeviceRoute::class,
                RootNavGraph.ReceiveSyncDeviceRoute.serializer(),
            )
            subclass(
                RootNavGraph.BlackListedDevicesRoute::class,
                RootNavGraph.BlackListedDevicesRoute.serializer(),
            )
            subclass(
                RootNavGraph.CreateDeviceBondRoute::class,
                RootNavGraph.CreateDeviceBondRoute.serializer(),
            )
        }
    }

    fun rootTabLayoutSerializer() = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(
                RootTabLayoutNavGraph.ListRoute::class,
                RootTabLayoutNavGraph.ListRoute.serializer(),
            )
            subclass(
                RootTabLayoutNavGraph.SettingsRoute::class,
                RootTabLayoutNavGraph.SettingsRoute.serializer(),
            )
            subclass(
                RootTabLayoutNavGraph.DeviceRoute::class,
                RootTabLayoutNavGraph.DeviceRoute.serializer(),
            )
        }
    }
}
