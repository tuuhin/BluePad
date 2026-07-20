package com.sam.bluepad.presentation.feature_settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.sam.bluepad.presentation.feature_settings.composable.SettingsScreenContent
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenEvent
import com.sam.bluepad.presentation.feature_settings.event.SettingsScreenState
import com.sam.bluepad.presentation.utils.BluepadPreviewWrapper
import com.sam.bluepad.presentation.utils.LocalSnackBarState
import com.sam.bluepad.resources.Res
import com.sam.bluepad.resources.ic_back
import com.sam.bluepad.resources.settings_screen_subtitle
import com.sam.bluepad.resources.settings_screen_title
import com.sam.bluepad.theme.Dimensions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEvent: (SettingsScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    deviceState: SettingsScreenState = SettingsScreenState(),
    navigation: @Composable () -> Unit = {}
) {
    val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackBarHostState = LocalSnackBarState.current


    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(text = stringResource(Res.string.settings_screen_title)) },
                subtitle = { Text(text = stringResource(Res.string.settings_screen_subtitle)) },
                navigationIcon = navigation,
                scrollBehavior = topBarScrollBehaviour,
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        contentWindowInsets = WindowInsets(),
        modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection),
    ) { padding ->
        SettingsScreenContent(
            state = deviceState,
            contentPadding = padding,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize()
                .padding(
                    horizontal = Dimensions.SCAFFOLD_HORIZONAL_PADDING,
                    vertical = Dimensions.SCAFFOLD_VERTICAL_PADDING,
                ),
        )
    }

}


@Preview
@PreviewWrapper(BluepadPreviewWrapper::class)
@Composable
private fun SettingsScreenPreview() = SettingsScreen(
    onEvent = {},
    navigation = {
        Icon(
            painter = painterResource(Res.drawable.ic_back),
            contentDescription = null,
        )
    },
)
