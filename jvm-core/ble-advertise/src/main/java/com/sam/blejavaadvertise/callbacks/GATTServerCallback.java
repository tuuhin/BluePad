package com.sam.blejavaadvertise.callbacks;

import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus;

public interface GATTServerCallback {

    void onServiceAdded(String serviceUuid, boolean success, int error);

    void onServiceStatusChange(GATTServiceAdvertisementStatus status);

    byte[] onReadCharacteristics(String serviceUuid, String characteristicUuid);
}
