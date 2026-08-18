package com.sam.bluepad.presentation.feature_settings.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowSizeClass
import com.sam.bluepad.domain.models.DevicePlatformOS
import com.sam.bluepad.domain.platform.PlatformDeviceInfo
import com.sam.bluepad.presentation.utils.LocalPlatformDetails
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_os_android
import com.sam.bluepad.resources.ic_os_mac
import com.sam.bluepad.resources.ic_os_unknown
import com.sam.bluepad.resources.ic_os_windows
import com.sam.bluepad.resources.scan_results_save_device_warning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformDetailsDialog(
    showDialog: Boolean,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties()
) {
    if (!showDialog) return

    val deviceInfo = LocalPlatformDetails.current
    val windowSize = LocalWindowSizeInfo.current

    val content = remember {
        movableContentOf { showDivider: Boolean ->
            PlatformLogo(deviceInfo)
            if (showDivider) {
                VerticalDivider(
                    modifier = Modifier.heightIn(max = 120.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            PlatformDetails(deviceInfo)
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismissDialog,
        modifier = modifier,
        properties = properties,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.extraExtraLarge,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    .wrapContentSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (!windowSize.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        content(false)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        content(true)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformDetails(
    deviceInfo: PlatformDeviceInfo,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        InfoRow(label = "Build", value = deviceInfo.buildNumber ?: "Unknown")
        InfoRow(label = "Architecture", value = deviceInfo.arch ?: "Unknown")
        InfoRow(label = "Bluetooth Adapter Name", value = deviceInfo.bluetoothAdapter ?: "Unknown")
        InfoRow(label = "Adapter Vendor", value = deviceInfo.bluetoothVendor ?: "Unknown")
        InfoRow(
            label = "MAC Address",
            value = deviceInfo.macAddress ?: "Unknown",
            isHidden = deviceInfo.isMacAddressHidden,
        )
    }
}

@Composable
private fun PlatformLogo(
    deviceInfo: PlatformDeviceInfo,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialShapes.Square.toShape(),
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
) {
    val deviceOs by remember(deviceInfo.osName) {
        derivedStateOf {
            val osName = deviceInfo.osName ?: return@derivedStateOf DevicePlatformOS.UNKNOWN
            when {
                osName.startsWith("Android", ignoreCase = true) -> DevicePlatformOS.ANDROID
                osName.startsWith("Windows", ignoreCase = true) -> DevicePlatformOS.WINDOWS
                osName.startsWith("Mac", ignoreCase = true) -> DevicePlatformOS.MACOS
                else -> DevicePlatformOS.UNKNOWN
            }
        }
    }

    Column(
        modifier = modifier.widthIn(max = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 80.dp, minHeight = 80.dp)
                .clip(shape)
                .background(color = containerColor, shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = when (deviceOs) {
                    DevicePlatformOS.ANDROID -> painterResource(Res.drawable.ic_os_android)
                    DevicePlatformOS.WINDOWS -> painterResource(Res.drawable.ic_os_windows)
                    DevicePlatformOS.MACOS -> painterResource(Res.drawable.ic_os_mac)
                    DevicePlatformOS.UNKNOWN -> painterResource(Res.drawable.ic_os_unknown)
                },
                contentDescription = stringResource(Res.string.scan_results_save_device_warning),
                tint = contentColorFor(containerColor),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = deviceInfo.osName ?: "Unknown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = deviceInfo.osVersion ?: "Unknown",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,
) {
    Row(
        modifier = modifier.padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        if (isHidden) Text(
            text = value,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
        ) else Text(
            text = value,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}
