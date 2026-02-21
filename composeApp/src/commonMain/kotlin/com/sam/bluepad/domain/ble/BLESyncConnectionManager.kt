package com.sam.bluepad.domain.ble

import com.sam.bluepad.domain.ble.events.ConnectorSyncEvent
import com.sam.bluepad.domain.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

typealias ResourcesSyncDataEvents = Resource<ConnectorSyncEvent, Exception>

interface BLESyncConnectionManager : AutoCloseable {

    fun discoverAndConnect(timeout: Duration = 20.seconds): Flow<ResourcesSyncDataEvents>

}