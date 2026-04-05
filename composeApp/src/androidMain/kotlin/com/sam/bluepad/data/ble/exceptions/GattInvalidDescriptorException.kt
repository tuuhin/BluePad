package com.sam.bluepad.data.ble.exceptions

import android.bluetooth.BluetoothGattDescriptor

class GattInvalidDescriptorException(private val descriptor: BluetoothGattDescriptor) :
    Exception("CANNOT FIND DESCRIPTOR:${descriptor.uuid} WITH CHARACTERISTICS:${descriptor.characteristic.uuid} WITH SERVICE:${descriptor.characteristic.service.uuid}")
