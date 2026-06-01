package com.sam.ble_advertise.models

enum class BLEAdvertisementStatus(val code: Int) {
    Created(0),
    Stopped(1),
    Started(2),
    StartedWithoutAdvertisementData(3),
    Aborted(4),
    Unknown(-1);

}

// need to keep this open for the kne psi code generation
// seems like normal kotlin companion objects are not supported yet
fun bLEAdvertisementStatusFromInt(statusInt: Int): BLEAdvertisementStatus {
    return BLEAdvertisementStatus.entries.getOrNull(statusInt) ?: BLEAdvertisementStatus.Unknown
}
