package com.sam.bluepad.presentation.utils

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow

abstract class AppViewModel(coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)) :
	ViewModel(coroutineScope) {

	abstract val uiEvent: SharedFlow<UIEvents>
}