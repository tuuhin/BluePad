package com.sam.blejavaadvertise.callbacks;

import com.sam.blejavaadvertise.models.GATTBluetoothError;
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus;

public interface GATTServerCallback {

    void onServiceAdded(String serviceUuid, boolean success, GATTBluetoothError error);

    void onServiceStatusChange(GATTServiceAdvertisementStatus status);

    byte[] onReadCharacteristics(String deviceAddress, String serviceUuid, String characteristicUuid);

    void onWriteCharacteristicRequest(String deviceAddress, String serviceUuid, String characteristicUuid, byte[] value);
}
