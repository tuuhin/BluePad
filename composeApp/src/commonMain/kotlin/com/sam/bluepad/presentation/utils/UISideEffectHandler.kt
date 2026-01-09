package com.sam.bluepad.presentation.utils

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.tbib.compose_toast.native_toast.NativeShowToast
import io.github.tbib.compose_toast.native_toast.NativeToastType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer

@Composable
fun UiEventsHandler(
	eventsFlow: () -> Flow<UIEvents>,
	onNavigateBack: () -> Unit = {},
) {

	val lifecyleOwner = LocalLifecycleOwner.current
	val snackBarState = LocalSnackBarState.current


	val updatedOnNavigateBack by rememberUpdatedState(newValue = onNavigateBack)

	LaunchedEffect(key1 = lifecyleOwner) {
		lifecyleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			eventsFlow().buffer(2, BufferOverflow.DROP_OLDEST)
				.collect { event ->
					when (event) {

						is UIEvents.ShowSnackBarWithActions -> {
							val result = snackBarState.showSnackbar(
								message = event.message,
								actionLabel = event.actionText,
								withDismissAction = event.actionText != null,
								duration = if (event.long) SnackbarDuration.Long else SnackbarDuration.Short
							)
							when (result) {
								SnackbarResult.ActionPerformed -> event.action()
								else -> {}
							}
						}

						is UIEvents.ShowToast -> NativeShowToast.show(
							"Native toast Native toast Native toast Native toast Native toast",
							NativeToastType.SHORT
						)

						is UIEvents.ShowSnackBar -> snackBarState.showSnackbar(
							message = event.message,
							duration = SnackbarDuration.Short
						)

						UIEvents.PopScreen -> updatedOnNavigateBack()
					}
				}
		}
	}
}