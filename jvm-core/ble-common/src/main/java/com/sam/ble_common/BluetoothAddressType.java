package com.sam.ble_common;

public enum BluetoothAddressType {
    PUBLIC(0),
    RANDOM(1),
    UNSPECIFIED(2);

    private final int value;

    BluetoothAddressType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return switch (this) {
            case PUBLIC -> "Public";
            case RANDOM -> "Random";
            default -> "Unspecified";
        };
    }

    public static BluetoothAddressType fromInt(int value) {
        for (BluetoothAddressType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return UNSPECIFIED;
    }
}