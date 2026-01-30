package com.sam.blejavaadvertise.models;

public enum GattNotificationResult {
    SUCCESS(0),
    UNREACHABLE(1),
    PROTOCOL_ERROR(2),
    ACCESS_DENIED(3);

    private final int code;

    GattNotificationResult(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return switch (this) {
            case SUCCESS-> "SUCCESS";
            case UNREACHABLE -> "UN-REACHABLE";
            case PROTOCOL_ERROR -> "PROTOCOL ERROR";
            case ACCESS_DENIED -> "PERMISSION DENIED";
        };
    }

    public static GattNotificationResult fromInt(int value) {
        for (GattNotificationResult type : values()) {
            if (type.code == value) {
                return type;
            }
        }
        return ACCESS_DENIED;
    }
}
