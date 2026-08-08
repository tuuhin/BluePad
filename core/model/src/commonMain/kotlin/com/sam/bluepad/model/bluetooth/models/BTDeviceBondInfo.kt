package com.sam.bluepad.model.bluetooth.models

import com.sam.bluepad.model.bluetooth.enums.BTDeviceBondState

sealed interface BTDeviceBondInfo {
    data class BondState(val state: BTDeviceBondState) : BTDeviceBondInfo
    data class ConfirmPin(val string: String) : BTDeviceBondInfo
}
