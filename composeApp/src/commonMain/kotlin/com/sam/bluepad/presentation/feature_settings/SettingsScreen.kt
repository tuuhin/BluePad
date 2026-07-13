package com.sam.bluepad.presentation.feature_settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sam.bluepad.presentation.composables.ContentLoadingWrapper
import com.sam.bluepad.presentation.feature_settings.composable.SettingsScreenContent
import com.sam.bluepad.presentation.feature_settings.event.CurrentDeviceState
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenEvent
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.settings_screen_subtitle
import com.sam.bluepad.resources.settings_screen_title
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEvent: (SettingsScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    deviceState: CurrentDeviceState? = null,
    navigation: @Composable () -> Unit = {}
) {
    val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackBarHostState = LocalSnackBarState.current

    val isContentLoading by remember(deviceState) {
        derivedStateOf { deviceState == null }
    }

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(text = stringResource(Res.string.settings_screen_title)) },
                subtitle = { Text(text = stringResource(Res.string.settings_screen_subtitle)) },
                navigationIcon = navigation,
                scrollBehavior = topBarScrollBehaviour
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        contentWindowInsets = WindowInsets(),
        modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection),
    ) { padding ->
        ContentLoadingWrapper(
            content = deviceState,
            isLoading = isContentLoading,
            modifier = Modifier.padding(padding),
            onSuccess = { state ->
                SettingsScreenContent(
                    state = state,
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                        vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING
                    ),
                    onUpdateName = { name -> onEvent(SettingsScreenEvent.OnUpdateDeviceName(name)) },
                )
            },
            onFailed = {

            },
        )
    }
}
