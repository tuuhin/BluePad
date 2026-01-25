package com.sam.bluepad.di

import com.sam.bluepad.AppCommonViewModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEAdvertisementViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEConnectDeviceViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BLEScanDevicesViewModel
import com.sam.bluepad.presentation.feature_devices.viewmodel.BlackListedDevicesViewmodel
import com.sam.bluepad.presentation.feature_devices.viewmodel.ManageDeviceViewmodel
import com.sam.bluepad.presentation.feature_settings.SettingsViewmodel
import com.sam.bluepad.presentation.feature_sketches.viewmodel.AddSketchViewModel
import com.sam.bluepad.presentation.feature_sketches.viewmodel.SketchesViewmodel
import com.sam.bluepad.presentation.feature_sync.viewmodel.ReceiveSyncDevicesViewModel
import com.sam.bluepad.presentation.feature_sync.viewmodel.SendSyncViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
	viewModelOf(::AppCommonViewModel)
	viewModelOf(::ManageDeviceViewmodel)
	viewModelOf(::BLEAdvertisementViewmodel)
	viewModelOf(::BLEScanDevicesViewModel)
	viewModelOf(::BLEConnectDeviceViewmodel)
	viewModel { params ->
		AddSketchViewModel(
			sketchId = params.getOrNull(),
			repository = get<SketchesRepository>(),
			localDeviceProvider = get<LocalDeviceInfoProvider>()
		)
	}
	viewModelOf(::SketchesViewmodel)
	viewModelOf(::SettingsViewmodel)

	// sync
	viewModelOf(::SendSyncViewModel)
	viewModelOf(::ReceiveSyncDevicesViewModel)
	viewModelOf(::BlackListedDevicesViewmodel)
}