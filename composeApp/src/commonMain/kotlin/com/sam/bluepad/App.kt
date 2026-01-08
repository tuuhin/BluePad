package com.sam.bluepad

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sam.bluepad.domain.bluetooth.BluetoothStateProvider
import com.sam.bluepad.presentation.navigation.AppRootNavHost
import com.sam.bluepad.presentation.utils.LocalBluetoothState
import com.sam.bluepad.presentation.utils.LocalPostureInfo
import com.sam.bluepad.presentation.utils.LocalSharedTransitionScope
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.presentation.utils.LocalWindowSizeInfo
import org.koin.compose.koinInject

@Composable
fun App(modifier: Modifier = Modifier) {

	val snackBarHostState = remember { SnackbarHostState() }
	val windowInfo = currentWindowAdaptiveInfo()

	val bluetoothStateProvider = koinInject<BluetoothStateProvider>()
	// as false is our check case we consider it to be true
	val state by bluetoothStateProvider.bluetoothStatusFlow.collectAsStateWithLifecycle(true)

	Surface(
		color = MaterialTheme.colorScheme.background,
		modifier = modifier
	) {
		SharedTransitionLayout {
			CompositionLocalProvider(
				LocalSnackBarState provides snackBarHostState,
				LocalSharedTransitionScope provides this,
				LocalWindowSizeInfo provides windowInfo.windowSizeClass,
				LocalPostureInfo provides windowInfo.windowPosture,
				LocalBluetoothState provides state,
			) {
				AppRootNavHost()
			}
		}
	}
}
