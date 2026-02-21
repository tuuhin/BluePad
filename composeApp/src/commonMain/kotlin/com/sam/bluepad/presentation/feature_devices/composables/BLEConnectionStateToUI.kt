package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.runtime.Composable
import com.sam.bluepad.domain.ble.enums.BLEConnectionState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.external_device_conn_state_connected
import com.sam.bluepad.resources.external_device_conn_state_connecting
import com.sam.bluepad.resources.external_device_conn_state_disconnected
import com.sam.bluepad.resources.external_device_conn_state_disconnecting
import org.jetbrains.compose.resources.stringResource


val BLEConnectionState.readableString: String
	@Composable
	get() = when (this) {
		BLEConnectionState.CONNECTING -> stringResource(Res.string.external_device_conn_state_connecting)
		BLEConnectionState.CONNECTED -> stringResource(Res.string.external_device_conn_state_connected)
		BLEConnectionState.DISCONNECTING -> stringResource(Res.string.external_device_conn_state_disconnecting)
		BLEConnectionState.DISCONNECTED -> stringResource(Res.string.external_device_conn_state_disconnected)
	}