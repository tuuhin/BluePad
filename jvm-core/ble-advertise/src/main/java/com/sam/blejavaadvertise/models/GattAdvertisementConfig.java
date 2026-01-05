package com.sam.blejavaadvertise.models;

public record GattAdvertisementConfig(boolean discoverable, boolean connectable, byte[] serviceData) {

}