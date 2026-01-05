package com.sam.bluepad.presentation.feature_settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sam.bluepad.presentation.utils.LocalSnackBarState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	modifier: Modifier = Modifier,
	navigation: @Composable () -> Unit = {}
) {
	val topBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val snackBarHostState = LocalSnackBarState.current

	Scaffold(
		topBar = {
			MediumFlexibleTopAppBar(
				title = { Text(text = "Settings") },
				navigationIcon = navigation, scrollBehavior = topBarScrollBehaviour
			)
		},
		snackbarHost = { SnackbarHost(snackBarHostState) },
		modifier = modifier.nestedScroll(topBarScrollBehaviour.nestedScrollConnection)
	) { padding ->
		Column(
			modifier = Modifier.padding(padding)
		) {

		}
	}
}