package com.sam.bluepad.presentation.navigation.nav_graph

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface RootNavGraph : NavKey {

    @Serializable
    data class AddOrUpdateRoute(val sketchId: Uuid? = null) : RootNavGraph

    @Serializable
    data object AssociatedNavGraphRoute : RootNavGraph

    @Serializable
    data object AdvertiseDeviceRoute : RootNavGraph

    @Serializable
    data object SearchDeviceRoute : RootNavGraph

    @Serializable
    data class ConnectDeviceRoute(val address: String) : RootNavGraph

    @Serializable
    data class SyncConnectorRoute(val deviceId: Uuid) : RootNavGraph

    @Serializable
    data object ReceiveSyncDeviceRoute : RootNavGraph

    @Serializable
    data class SyncChangesListRouteEntry(val remoteDeviceId: Uuid, val sessionId: Uuid) : RootNavGraph

    @Serializable
    data object BlackListedDevicesRoute : RootNavGraph

    @Serializable
    data class CreateDeviceBondRoute(
        val identifier: String,
        val localName: String? = null
    ) : RootNavGraph
}
