package com.sam.bluepad.presentation.feature_sketches.viewmodel

import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.interactions.CopySketchInteraction
import com.sam.bluepad.domain.interactions.ShareSketchInteraction
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class SketchesViewmodel(
	private val repository: SketchesRepository,
	private val localDeviceProvider: LocalDeviceInfoProvider,
	private val shareInteraction: ShareSketchInteraction,
	private val copyInteraction: CopySketchInteraction,
) : AppViewModel() {

	private val _selectedSketch = MutableStateFlow<SketchModel?>(null)
	val isSketchSelected = _selectedSketch.map { it != null }.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Lazily,
		initialValue = false
	)

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
			is SketchScreenEvent.OnSelectSketchToDelete -> _selectedSketch.update { event.sketch }
			SketchScreenEvent.OnUnselectSketchToDelete -> _selectedSketch.update { null }
			SketchScreenEvent.OnDeleteSketchConfirm -> onDeleteSketch()
			is SketchScreenEvent.OnCopySketch -> onCopySketch(event.sketch)
			is SketchScreenEvent.OnShareSketch -> onShareSketch(event.sketch)
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

	private fun onDeleteSketch() = viewModelScope.launch {
		val localDeviceId = readLocalDeviceId() ?: return@launch
		val cachedModel = _selectedSketch.value ?: return@launch

		// show inform about delete
		_uiEvents.emit(UIEvents.ShowToast("Deleting Sketch"))
		//start delete
		repository.revokeSketch(cachedModel, localDeviceId)
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "Some error"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> _selectedSketch.update { null }
					else -> {}
				}
			}
			.launchIn(this)
	}

	private suspend fun readLocalDeviceId(): Uuid? {
		val localDeviceId = withTimeoutOrNull(2.seconds) {
			localDeviceProvider.readDeviceId.first()
		}
		if (localDeviceId == null) _uiEvents.emit(UIEvents.ShowSnackBar("Cannot read device id"))
		return localDeviceId
	}

	private fun onCopySketch(sketch: SketchModel) = viewModelScope.launch {
		val result = copyInteraction.copyToClipboard(sketch)
		result.fold(
			onSuccess = { showMessage ->
				if (!showMessage) return@fold
				_uiEvents.emit(UIEvents.ShowToast("Content copied"))
			},
			onFailure = { err ->
				val message = err.message ?: "Copy sketch failed"
				_uiEvents.emit(UIEvents.ShowSnackBar(message))
			},
		)
	}

	private fun onShareSketch(sketch: SketchModel) = viewModelScope.launch {
		val result = shareInteraction.shareSketch(sketch)
		if (result.isFailure) {
			val error = result.exceptionOrNull()
			val errorMessage = error?.message ?: "Failed to share sketch"
			_uiEvents.emit(UIEvents.ShowSnackBar(errorMessage))
		}
	}
}