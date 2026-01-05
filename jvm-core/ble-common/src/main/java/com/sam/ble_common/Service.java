package com.sam.ble_common;

import java.util.List;

public record Service(String uuid, byte[] data, List<Characteristic> characteristics) {
}