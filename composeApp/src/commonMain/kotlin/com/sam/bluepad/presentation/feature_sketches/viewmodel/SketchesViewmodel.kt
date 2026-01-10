package com.sam.bluepad.presentation.feature_sketches.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_sketches.events.SketchScreenEvent
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SketchesViewmodel(
	private val repository: SketchesRepository
) : AppViewModel() {

	private val _devices = MutableStateFlow(emptyList<SketchModel>())
	val sketches = _devices
		.onStart { loadDevicesList() }
		.map { it.toImmutableList() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000L),
			initialValue = persistentListOf()
		)

	private val _isLoading = MutableStateFlow(true)
	val isLoading = _isLoading.asStateFlow()

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents.asSharedFlow()


	fun onEvent(event: SketchScreenEvent) {
		when (event) {
			is SketchScreenEvent.OnDeleteSketch -> {}
		}
	}

	private fun loadDevicesList() = repository.getSketches()
		.onEach { res ->
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "Some error"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
					_isLoading.update { false }
				}

				is Resource.Success -> {
					_devices.update { res.data }
					_isLoading.update { false }
				}

				Resource.Loading -> _isLoading.update { true }
			}
		}
		.launchIn(viewModelScope)
}