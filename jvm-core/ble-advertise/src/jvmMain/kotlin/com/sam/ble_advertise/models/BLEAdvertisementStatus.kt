package com.sam.ble_advertise.models

enum class BLEAdvertisementStatus(val code: Int) {
    Created(0),
    Stopped(1),
    Started(2),
    StartedWithoutAdvertisementData(3),
    Aborted(4),
    Unknown(-1);

    companion object {
        fun bLEAdvertisementStatusFromInt(statusInt: Int): BLEAdvertisementStatus {
            return entries.getOrNull(statusInt) ?: Unknown
        }
    }
}


