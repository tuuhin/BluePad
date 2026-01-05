package com.sam.bluepad.data.ble

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

internal class ServerAdvertisementCallback : AdvertiseCallback() {

	private val _isRunning = MutableStateFlow(false)
	val isRunning = _isRunning.asStateFlow()

	private val _errorsChannel = Channel<Exception>(capacity = Channel.CONFLATED)
	val errorFlow = _errorsChannel.receiveAsFlow()

	override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
		super.onStartSuccess(settingsInEffect)
		_isRunning.value = true
	}

	override fun onStartFailure(errorCode: Int) {
		super.onStartFailure(errorCode)
		val exception = when (errorCode) {
			ADVERTISE_FAILED_ALREADY_STARTED -> Exception("Advertisement is already running")
			ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Exception("Too many advertiser")
			ADVERTISE_FAILED_INTERNAL_ERROR -> Exception("Android cannot start advertisement")
			ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> Exception("BLE not supported")
			else -> Exception("Cannot start the advertisement")
		}
		_errorsChannel.trySend(exception)
	}
}