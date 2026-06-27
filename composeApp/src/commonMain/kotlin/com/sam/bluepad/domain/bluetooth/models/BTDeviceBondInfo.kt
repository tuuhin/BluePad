package com.sam.bluepad.domain.bluetooth.models

import com.sam.bluepad.domain.bluetooth.enums.BTDeviceBondState

sealed interface BTDeviceBondInfo {
    data class BondState(val state: BTDeviceBondState) : BTDeviceBondInfo
    data class ConfirmPin(val string: String) : BTDeviceBondInfo
}
