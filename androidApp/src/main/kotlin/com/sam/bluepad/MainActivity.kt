package com.sam.bluepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.settings.models.AppFontOption
import com.sam.bluepad.presentation.feature_settings.SettingsViewmodel
import com.sam.bluepad.theme.BluePadTheme
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val _permissionController by inject<PermissionsController>()
    private val _localDeviceProvider by inject<LocalDeviceInfoProvider>()

    private val _userSettings by viewModel<SettingsViewmodel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // edge to edge
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // enable edge to edge
        enableEdgeToEdge()

        //initiate device data
        lifecycleScope.launch { _localDeviceProvider.initiateDeviceInfo() }

        // bind to this activity
        _permissionController.bind(this)

        // Keep splash screen visible until settings are loaded
        splash.setKeepOnScreenCondition { _userSettings.isSettingsLoaded.value }

        setContent {
            val settings by _userSettings.state.collectAsStateWithLifecycle()

            BluePadTheme(
                dynamicColor = settings.appSettings.useDynamicColor,
                useSystemFonts = settings.appSettings.fontOption == AppFontOption.SYSTEM,
            ) {
                App()
            }
        }
    }
}
