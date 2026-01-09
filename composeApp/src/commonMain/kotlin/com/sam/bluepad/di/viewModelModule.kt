package com.sam.bluepad.di

import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEAdvertisementViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEConnectDeviceViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEScanDevicesViewModel
import com.sam.bluepad.presentation.feature_devices.viewmodel.ManageDeviceViewmodel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
	viewModelOf(::ManageDeviceViewmodel)
	viewModelOf(::BLEAdvertisementViewmodel)
	viewModelOf(::BLEScanDevicesViewModel)
	viewModelOf(::BLEConnectDeviceViewmodel)
}