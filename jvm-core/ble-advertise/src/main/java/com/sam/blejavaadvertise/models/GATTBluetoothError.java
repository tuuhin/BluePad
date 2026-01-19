package com.sam.blejavaadvertise.models;

public enum GATTBluetoothError {
    SUCCESS(0),
    RADIO_NOT_AVAILABLE(1),
    RESOURCE_IN_USE(2),
    DEVICE_NOT_CONNECTED(3),
    OTHER_ERROR(4),
    DISABLED_BY_POLICY(5),
    NOT_SUPPORTED(6),
    DISABLED_BY_USER(7),
    CONSENT_REQUIRED(8),
    TRANSPORT_NOT_SUPPORTED(9);

    private final int code;

    GATTBluetoothError(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return switch (this) {
            case SUCCESS -> "SUCCESS";
            case RADIO_NOT_AVAILABLE -> "RADIO NOT AVAILABLE";
            case RESOURCE_IN_USE -> "RESOURCE IN USE";
            case DEVICE_NOT_CONNECTED -> "DEVICE NOT CONNECTED";
            case OTHER_ERROR -> "OTHER";
            case DISABLED_BY_POLICY -> "DISABLED BY POLICY";
            case NOT_SUPPORTED -> "NOT SUPPORTED";
            case DISABLED_BY_USER -> "DISABLED BY USER";
            case CONSENT_REQUIRED -> "CONSENT NOT PROVIDED";
            case TRANSPORT_NOT_SUPPORTED -> "TRANSPORT NOT SUPPORTED";
        };
    }

    public static GATTBluetoothError fromInt(int status) {
        for (GATTBluetoothError type : values()) {
            if (type.code == status) {
                return type;
            }
        }
        return OTHER_ERROR;
    }
}
