package com.sam.ble_advertise.models

data class BLECharacteristicsModel(
    val characteristicUuid: String,
    val canRead: Boolean = false,
    val canWrite: Boolean = false,
    val canWriteNoResponse: Boolean = false,
    val canNotify: Boolean = false,
    val canIndicate: Boolean = false,
)
