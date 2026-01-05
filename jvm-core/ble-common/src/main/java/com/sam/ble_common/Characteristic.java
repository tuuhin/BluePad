package com.sam.ble_common;

import java.util.List;

public record Characteristic(
        String uuid,
        List<Descriptor> descriptors,
        boolean canRead,
        boolean canWriteRequest,
        boolean canWriteCommand,
        boolean canNotify,
        boolean canIndicate) {
}