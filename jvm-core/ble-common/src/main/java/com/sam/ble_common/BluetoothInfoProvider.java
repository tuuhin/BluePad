package com.sam.ble_common;

import java.io.IOException;

public class BluetoothInfoProvider {

    public boolean getBluetoothStatus() {
        return nativeIsBluetoothActive();
    }

    public void registerCallback(BluetoothStateListener callback) {
        BluetoothListenerInternal listener = callback::onStatusChange;
        nativeRegisterBTListener(listener);
    }

    public void unregisterCallback() {
        unregisterBTListener();
    }

    private native void nativeRegisterBTListener(BluetoothListenerInternal callback);

    private native void unregisterBTListener();

    private native static boolean nativeIsBluetoothActive();

    interface BluetoothListenerInternal {

        void onStatusChange(boolean isActive);
    }

    public static boolean isBluetoothActive() {
        return nativeIsBluetoothActive();
    }

    static {
        try {
            NativeLibraryLoader.loadLibrary("bt_common");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    }
}
