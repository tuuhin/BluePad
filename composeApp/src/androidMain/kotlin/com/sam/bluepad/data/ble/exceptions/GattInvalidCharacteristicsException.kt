package com.sam.bluepad.data.ble.exceptions

import android.bluetooth.BluetoothGattCharacteristic

class GattInvalidCharacteristicsException(characteristic: BluetoothGattCharacteristic) :
    Exception("CANNOT FIND ANY CHARACTERISTICS:${characteristic.uuid} WITH SERVICE:${characteristic.service.uuid}")
