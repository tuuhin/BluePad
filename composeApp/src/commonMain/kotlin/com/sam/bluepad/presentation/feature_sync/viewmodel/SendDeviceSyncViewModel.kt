package com.sam.bluepad.presentation.feature_sync.viewmodel

import com.sam.bluepad.presentation.feature_sync.event.SendDeviceSyncScreenEvents
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class SendDeviceSyncViewModel : AppViewModel() {

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	fun onEvent(event: SendDeviceSyncScreenEvents) {

	}
}