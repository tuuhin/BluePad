package com.sam.bluepad.presentation.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_os_android
import com.sam.bluepad.resources.ic_os_unknown
import com.sam.bluepad.resources.ic_os_windows
import com.sam.bluepad.resources.scan_results_save_device_warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun DeviceOSTypeContainer(
	deviceOs: DevicePlatformOS = DevicePlatformOS.ANDROID,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier
			.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
			.background(
				color = MaterialTheme.colorScheme.primaryContainer,
				shape = when (deviceOs) {
					DevicePlatformOS.ANDROID -> MaterialShapes.Cookie4Sided.toShape()
					DevicePlatformOS.WINDOWS -> MaterialShapes.Square.toShape()
					DevicePlatformOS.UNKNOWN -> MaterialShapes.Arch.toShape()
				},
			),
		contentAlignment = Alignment.Center,
	) {
		Icon(
			painter = when (deviceOs) {
				DevicePlatformOS.ANDROID -> painterResource(Res.drawable.ic_os_android)
				DevicePlatformOS.WINDOWS -> painterResource(Res.drawable.ic_os_windows)
				DevicePlatformOS.UNKNOWN -> painterResource(Res.drawable.ic_os_unknown)
			},
			contentDescription = stringResource(Res.string.scan_results_save_device_warning),
			tint = MaterialTheme.colorScheme.onPrimaryContainer,
			modifier = Modifier.size(24.dp)
		)
	}
}