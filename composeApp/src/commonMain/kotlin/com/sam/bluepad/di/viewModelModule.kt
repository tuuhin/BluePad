package com.sam.bluepad.di

import com.sam.bluepad.presentation.core.AppCommonViewModel
import com.sam.bluepad.presentation.feature_bond.CreateDeviceBondViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEAdvertisementViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEConnectDeviceViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEScanDevicesViewModel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BlackListedDevicesViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.ManageDeviceViewmodel
import com.sam.bluepad.presentation.feature_settings.SettingsViewmodel
import com.sam.bluepad.presentation.feature_sketches.viewmodel.AddSketchViewModel
import com.sam.bluepad.presentation.feature_sketches.viewmodel.SketchesViewmodel
import com.sam.bluepad.presentation.feature_sync.viewmodel.ReviewSyncChangesViewModel
import com.sam.bluepad.presentation.feature_sync.viewmodel.SyncConnectorViewModel
import com.sam.bluepad.presentation.feature_sync.viewmodel.SyncReceiverViewmodel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    viewModel<AppCommonViewModel>()
    viewModel<ManageDeviceViewmodel>()
    viewModel<BLEAdvertisementViewmodel>()
    viewModel<BLEScanDevicesViewModel>()
    viewModel<BLEConnectDeviceViewmodel>()
    viewModel<AddSketchViewModel>()
    viewModel<SketchesViewmodel>()
    viewModel<SettingsViewmodel>()

    // sync
    viewModel<SyncConnectorViewModel>()
    viewModel<SyncReceiverViewmodel>()
    viewModel<BlackListedDevicesViewmodel>()
    viewModel<ReviewSyncChangesViewModel>()

    // bond
    viewModel<CreateDeviceBondViewmodel>()
}
