package com.sam.bluepad.presentation.feature_sync.state

enum class ConnectorDiscoveryState {
    DISCOVERING,
    TIMEOUT,
    DISCOVERED,
    DISCONNECTED
}