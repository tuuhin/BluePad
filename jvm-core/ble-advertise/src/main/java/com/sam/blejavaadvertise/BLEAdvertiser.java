package com.sam.blejavaadvertise;

import com.sam.ble_common.NativeLibraryLoader;
import com.sam.ble_common.Service;
import com.sam.ble_common.ShutdownThread;
import com.sam.blejavaadvertise.callbacks.GATTServerCallback;
import com.sam.blejavaadvertise.models.GATTBluetoothError;
import com.sam.blejavaadvertise.models.GATTServiceAdvertisementStatus;
import com.sam.blejavaadvertise.models.GattAdvertisementConfig;

import java.io.IOException;

public class BLEAdvertiser {

    private long nativeHandler;

    private GATTServerCallback listener;

    public void startServer() throws NativeDestroyNotCalledException {
        if (nativeHandler != 0) throw new NativeDestroyNotCalledException();
        this.nativeHandler = nativeCreate();
        GATTServerCallbackInternal callback = new GATTServerCallbackInternal() {

            @Override
            public byte[] onReadCharacteristics(String deviceAddress, String serviceUuid, String characteristicUuid) {
                if (listener == null) return new byte[0];
                return listener.onReadCharacteristics(serviceUuid, serviceUuid, characteristicUuid);
            }

            @Override
            public void onWriteCharacteristicRequest(String deviceAddress, String serviceUuid, String characteristicUuid, byte[] value) {
                if (listener == null) return;
                listener.onWriteCharacteristicRequest(deviceAddress, serviceUuid, characteristicUuid, value);
            }

            @Override
            public void onServiceStatusChange(int status) {
                if (listener == null) return;
                listener.onServiceStatusChange(GATTServiceAdvertisementStatus.fromInt(status));
            }

            @Override
            public void onServiceAdded(String serviceUuid, int error) {
                if (listener == null) return;
                listener.onServiceAdded(serviceUuid, error == 0, GATTBluetoothError.fromInt(error));
            }
        };
        nativeRegisterCallback(nativeHandler, callback);
    }

    public void addService(Service service) {
        ensureValid();
        nativeAddService(nativeHandler, service);
    }

    public void startAdvertisement(GattAdvertisementConfig data) {
        ensureValid();
        nativeStartAdvertising(nativeHandler, data);
    }

    public GATTServiceAdvertisementStatus getAdvertisementStatus() {
        ensureValid();
        int status = nativeGetAdvertisingStatus(nativeHandler);
        return GATTServiceAdvertisementStatus.fromInt(status);
    }


    public void stopAdvertisement() {
        if (nativeHandler != 0) nativeStopAdvertising(nativeHandler);
    }

    public void stopServer() {
        if (nativeHandler == 0) return;
        try {
            nativeStopAdvertising(nativeHandler);
        } finally {
            nativeDestroy(nativeHandler);
            nativeHandler = 0;
        }
    }

    public void setListener(GATTServerCallback listener) {
        this.listener = listener;
    }

    private void ensureValid() {
        if (nativeHandler == 0) throw new IllegalStateException("BLE server not started");
    }

    private native long nativeCreate();

    private native void nativeRegisterCallback(long h, GATTServerCallbackInternal callback);

    private native void nativeDestroy(long h);

    private native void nativeAddService(long h, Service service);

    private native void nativeStartAdvertising(long h, GattAdvertisementConfig data);

    private native void nativeStopAdvertising(long h);

    private native int nativeGetAdvertisingStatus(long h);

    interface GATTServerCallbackInternal {

        void onServiceAdded(String serviceUuid, int error);

        void onServiceStatusChange(int status);

        byte[] onReadCharacteristics(String deviceAddress, String serviceUuid, String characteristicUuid);

        void onWriteCharacteristicRequest(String deviceAddress, String serviceUuid, String characteristicUuid, byte[] value);
    }

    static {
        try {
            NativeLibraryLoader.loadLibrary("ble_java_advertise");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    }
}