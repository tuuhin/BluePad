package com.sam.blejavaadvertise.models;

public enum GATTServiceAdvertisementStatus {
    CREATED(0),
    STOPPED(1),
    STARTED(2),
    ABORTED(3),
    STARTED_WITHOUT_ADVERTISEMENT(4);

    private final int code;

    GATTServiceAdvertisementStatus(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return switch (this) {
            case CREATED -> "CREATED";
            case STARTED -> "STARTED";
            case STOPPED -> "STOPPED";
            case ABORTED -> "ABORTED";
            case STARTED_WITHOUT_ADVERTISEMENT -> "STARTED BUT WITHOUT ADVERTISEMENT";
        };
    }

    public static GATTServiceAdvertisementStatus fromInt(int value) {
        for (GATTServiceAdvertisementStatus type : values()) {
            if (type.code == value) {
                return type;
            }
        }
        return ABORTED;
    }
}
