package com.sam.bluepad.presentation.feature_devices.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.add_devices_screen_subtitle
import com.sam.bluepad.resources.add_devices_screen_title
import com.sam.bluepad.resources.ic_refresh
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddDeviceScreenTopBar(
    onRefreshItems: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier,
    isScanRunning: Boolean = false,
    isRefreshButtonEnabled: Boolean = false,
    navigation: @Composable () -> Unit,
    topBarScrollBehaviour: TopAppBarScrollBehavior? = null
) {
    MediumFlexibleTopAppBar(
        title = { Text(text = stringResource(Res.string.add_devices_screen_title)) },
        subtitle = { Text(text = stringResource(Res.string.add_devices_screen_subtitle)) },
        navigationIcon = navigation,
        scrollBehavior = topBarScrollBehaviour,
        modifier = modifier,
        actions = {
            OutlinedButton(
                onClick = onRefreshItems,
                enabled = isRefreshButtonEnabled,
                contentPadding = ButtonDefaults.SmallContentPadding,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_refresh),
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            BLEScanStartStopButton(
                isScanning = isScanRunning,
                onStopScan = onStopScan,
                onStartScan = onStartScan,
            )
            Spacer(modifier = Modifier.width(8.dp))
        },
    )
}
