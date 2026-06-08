package com.sam.ble_advertise.models

enum class GattWriteResponse(val code: Int) {
    SUCCESS(0),
    FAILED(1);
}
