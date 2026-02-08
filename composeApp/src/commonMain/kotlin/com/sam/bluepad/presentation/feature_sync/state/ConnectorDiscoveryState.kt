package com.sam.bluepad.presentation.feature_sync.state

enum class ConnectorDiscoveryState {
    NOT_STARTED,
    DISCOVERING,
    TIMEOUT,
    DISCOVERED,
    DISCONNECTED,
}