package com.sam.bluepad.presentation.feature_devices.events

sealed interface AdvertisementScreenEvent {
	data object OnStartAdvertise : AdvertisementScreenEvent
	data object OnStopAdvertise : AdvertisementScreenEvent
}