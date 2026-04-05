package com.sam.blejavaadvertise.callbacks;

import com.sam.blejavaadvertise.models.GATTBluetoothError;
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus;
import com.sam.blejavaadvertise.models.GattNotificationResult;

public interface GATTServerCallback {

    void onServiceAdded(String serviceUuid, boolean success, GATTBluetoothError error);

    void onServiceStatusChange(GATTServiceAdvertisementStatus status);

    byte[] onReadCharacteristics(String deviceAddress, String serviceUuid, String characteristicUuid);

    void onWriteCharacteristicRequest(String deviceAddress, String serviceUuid, String characteristicUuid, byte[] value);

    default byte[] onReadDescriptor(String deviceAddress, String serviceUuid, String characteristicsUuid, String descriptorUuid) {
        return null;
    }

    default void onWriteDescriptor(String deviceAddress, String serviceUuid, String characteristicsUuid, String descriptorUuid, byte[] value) {
    }

    default void onNotificationResultSuccess(String deviceAddress, String characteristicsUuid, GattNotificationResult result) {
    }

    default void onNotificationResultFailed(String deviceAddress, String characteristicsUuid, int errorCode) {

    }
}
