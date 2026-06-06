package com.sam.bluepad.data.ble.callbacks

import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import co.touchlab.kermit.Logger
import com.sam.bluepad.domain.exceptions.BLEAdvertisementException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

private const val TAG = "BLE_ADVERTISEMENT_CALLBACK"

class BLEGattAdvertisementCallback : AdvertisingSetCallback() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _errorsChannel = Channel<Exception>(capacity = Channel.CONFLATED)
    val errorsFlow = _errorsChannel.receiveAsFlow()

    override fun onAdvertisingSetStarted(
        advertisingSet: AdvertisingSet?,
        txPower: Int,
        status: Int
    ) {
        if (status != ADVERTISE_SUCCESS) {
            onStartFailure(status)
            return
        }
        val powerLevel = when (txPower) {
            -21 -> "TX_POWER LEVEL ULTRA LOW"
            -15 -> "TX POWER LEVEL LOW"
            -7 -> "TX POWER LEVEL MEDIUM"
            1 -> "TX POWER LEVEL HIGH"
            else -> null
        }
        Logger.d(tag = TAG) { "BLE5 ADVERTISING STARTED | $powerLevel" }
        _isRunning.value = true
    }

    override fun onAdvertisingEnabled(
        dvertisingSet: AdvertisingSet?, enable: Boolean, status: Int
    ) {
        if (status != ADVERTISE_SUCCESS || !enable) return
        Logger.d(tag = TAG) { "BLE5 ADVERTISING ENABLED" }
    }

    override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
        Logger.d(tag = TAG) { "BLE5 ADVERTISING STOPPED" }
        _isRunning.value = false
    }

    private fun onStartFailure(errorCode: Int) {
        val exception = when (errorCode) {
            ADVERTISE_FAILED_ALREADY_STARTED -> BLEAdvertisementException("Advertisement is already running")
            ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> BLEAdvertisementException("Too many advertiser")
            ADVERTISE_FAILED_INTERNAL_ERROR -> BLEAdvertisementException("Android cannot start advertisement")
            ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> BLEAdvertisementException("BLE not supported")
            else -> BLEAdvertisementException("Cannot start the advertisement ERROR CODE: $errorCode")
        }
        Logger.e(tag = TAG, throwable = exception) { "GATT SERVER FAILED TO START ERROR CODE: $errorCode" }
        _errorsChannel.trySend(exception)
    }

    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }

}
