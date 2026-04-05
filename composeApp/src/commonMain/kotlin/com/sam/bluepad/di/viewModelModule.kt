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
import com.sam.bluepad.presentation.feature_sync.viewmodel.SyncConnectorViewModel
import com.sam.bluepad.presentation.feature_sync.viewmodel.SyncReceiverViewmodel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.uuid.Uuid

val viewModelModule = module {
	viewModelOf(::AppCommonViewModel)
	viewModelOf(::ManageDeviceViewmodel)
	viewModelOf(::BLEAdvertisementViewmodel)
	viewModelOf(::BLEScanDevicesViewModel)
	viewModelOf(::BLEConnectDeviceViewmodel)
	viewModel { (sketchId: Uuid?) ->
		AddSketchViewModel(
			sketchId = sketchId,
			repository = get<SketchesRepository>(),
			localDeviceProvider = get<LocalDeviceInfoProvider>()
		)
	}
	viewModelOf(::SketchesViewmodel)
	viewModelOf(::SettingsViewmodel)

	// sync
	viewModelOf(::SyncConnectorViewModel)
	viewModelOf(::SyncReceiverViewmodel)
	viewModelOf(::BlackListedDevicesViewmodel)
}