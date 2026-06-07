package com.sam.ble_advertise.models

enum class BLEProtectionLevel {
    PLAIN,
    ENCRYPTION_REQUIRED,
    AUTHENTICATION_REQUIRED,
    ENCRYPTION_AND_AUTHENTICATION_REQUIRED;
}
