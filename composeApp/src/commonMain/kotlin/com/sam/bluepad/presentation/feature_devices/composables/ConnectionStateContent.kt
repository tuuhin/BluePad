package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.ble.enums.BLEConnectionState
import com.sam.bluepad.domain.ble.models.BLEPeerData
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.external_device_conn_device_data_collection_started
import com.sam.bluepad.resources.external_device_conn_device_data_found
import com.sam.bluepad.resources.external_device_conn_device_disconnected
import com.sam.bluepad.resources.ic_device_connecting
import com.sam.bluepad.resources.ic_device_success
import com.sam.bluepad.resources.ic_disconnected
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConnectionStateContent(
	connectionState: BLEConnectionState,
	modifier: Modifier = Modifier,
	peerData: BLEPeerData? = null,
	textStyle: TextStyle = MaterialTheme.typography.bodyLargeEmphasized,
	textContentColor: Color = MaterialTheme.colorScheme.onSurface,
	iconContainerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
	spacing: Dp = 12.dp,
) {

	val state by remember(connectionState, peerData) {
		derivedStateOf {
			when (connectionState) {
				BLEConnectionState.CONNECTED if peerData != null ->
					BLEConnectionDeviceData.ContainsData(peerData)

				BLEConnectionState.CONNECTED -> BLEConnectionDeviceData.ConnectedWithoutData
				BLEConnectionState.DISCONNECTED if peerData != null ->
					BLEConnectionDeviceData.ContainsData(peerData)

				BLEConnectionState.DISCONNECTED -> BLEConnectionDeviceData.Disconnected
				else -> BLEConnectionDeviceData.ConnectingOrDisconnecting
			}
		}
	}

	Box(
		modifier = modifier.sizeIn(minWidth = 120.dp, minHeight = 120.dp),
		contentAlignment = Alignment.Center
	) {
		Crossfade(
			targetState = state,
			animationSpec = tween(durationMillis = 200, delayMillis = 60, easing = EaseInOut)
		) { peerState ->
			when (peerState) {
				BLEConnectionDeviceData.ConnectingOrDisconnecting -> {
					Column(
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.wrapContentSize(),
                    ) {
						LoadingIndicator(color = MaterialTheme.colorScheme.tertiaryContainer)
						Text(
							text = connectionState.readableString,
							style = textStyle,
							color = textContentColor
						)
					}
				}

				BLEConnectionDeviceData.ConnectedWithoutData -> {
					Column(
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.wrapContentSize(),
                    ) {
						Box(
							modifier = Modifier.background(
								color = iconContainerColor,
								shape = MaterialShapes.Cookie6Sided.toShape(),
							),
							contentAlignment = Alignment.Center,
						) {
							Icon(
								painter = painterResource(Res.drawable.ic_device_connecting),
								contentDescription = connectionState.readableString,
								modifier = Modifier.size(64.dp).padding(12.dp),
								tint = contentColorFor(iconContainerColor)
							)
						}
						Text(
							text = stringResource(Res.string.external_device_conn_device_data_collection_started),
							style = textStyle,
							color = textContentColor, textAlign = TextAlign.Center,
						)
					}
				}

				is BLEConnectionDeviceData.ContainsData -> {
					Column(
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.wrapContentSize(),
                    ) {
						Box(
							modifier = Modifier.background(
								color = iconContainerColor,
								shape = MaterialShapes.Pill.toShape(),
							),
							contentAlignment = Alignment.Center,
						) {
							Icon(
								painter = painterResource(Res.drawable.ic_device_success),
								contentDescription = stringResource(Res.string.external_device_conn_device_data_found),
								modifier = Modifier.size(64.dp)
									.padding(12.dp),
								tint = contentColorFor(iconContainerColor)
							)
						}
						Text(
							text = stringResource(Res.string.external_device_conn_device_data_found),
							style = textStyle,
							color = textContentColor
						)
					}
				}

				BLEConnectionDeviceData.Disconnected -> {
					Column(
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.wrapContentSize(),
                    ) {
						Box(
							modifier = Modifier.background(
								color = iconContainerColor,
								shape = MaterialShapes.Oval.toShape(),
							),
							contentAlignment = Alignment.Center,
						) {
							Icon(
								painter = painterResource(Res.drawable.ic_disconnected),
								contentDescription = stringResource(Res.string.external_device_conn_device_disconnected),
								modifier = Modifier.size(64.dp).padding(12.dp),
								tint = contentColorFor(iconContainerColor)
							)
						}
						Text(
							text = stringResource(Res.string.external_device_conn_device_disconnected),
							style = textStyle,
							color = textContentColor,
							textAlign = TextAlign.Center
						)
					}
				}
			}
		}
	}
}

private sealed class BLEConnectionDeviceData {
	data object ConnectingOrDisconnecting : BLEConnectionDeviceData()
	data object ConnectedWithoutData : BLEConnectionDeviceData()
	data class ContainsData(val data: BLEPeerData) : BLEConnectionDeviceData()
	data object Disconnected : BLEConnectionDeviceData()
}
