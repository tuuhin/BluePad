package com.sam.bluepad.presentation.feature_sketches.viewmodel

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.viewModelScope
import com.sam.bluepad.domain.models.CreateSketchModel
import com.sam.bluepad.domain.models.SketchModel
import com.sam.bluepad.domain.provider.LocalDeviceInfoProvider
import com.sam.bluepad.domain.repository.SketchesRepository
import com.sam.bluepad.domain.utils.Resource
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchScreenEvent
import com.sam.bluepad.presentation.feature_sketches.events.CreateSketchState
import com.sam.bluepad.presentation.utils.AppViewModel
import com.sam.bluepad.presentation.utils.UIEvents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class AddSketchViewModel(
	private val sketchId: Uuid?,
	private val repository: SketchesRepository,
	private val localDeviceProvider: LocalDeviceInfoProvider,
) : AppViewModel() {

	private val _screenState = MutableStateFlow(CreateSketchState())
	val screenState = _screenState.asStateFlow()

	private val _isContentLoadFailed = MutableStateFlow(false)
	val isContentLoadFailed = _isContentLoadFailed.asStateFlow()

	private val _loadedSketch = MutableStateFlow<SketchModel?>(null)

	private val _isLoading = MutableStateFlow(sketchId != null)
	val isLoading = _isLoading
		.onStart { onLoadContent() }
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = sketchId != null
		)

	private val _uiEvents = MutableSharedFlow<UIEvents>()
	override val uiEvent: SharedFlow<UIEvents>
		get() = _uiEvents

	fun onEvent(event: CreateSketchScreenEvent) {
		when (event) {
			CreateSketchScreenEvent.OnSaveSketch -> onSaveSketch()
			CreateSketchScreenEvent.OnUpdateSketch -> onUpdateSketch()
			CreateSketchScreenEvent.OnConfirmDeleteSketch -> onDeleteSketch()
			CreateSketchScreenEvent.OnToggleDeleteDialog -> _screenState.update { state ->
				state.copy(showDeleteDialog = !state.showDeleteDialog)
			}
		}
	}

	private fun onLoadContent() = viewModelScope.launch {
		val sketchIdUuid = sketchId ?: run {
			// new sketch
			_screenState.update { state -> state.copy(isNewContent = true) }
			return@launch
		}
		// old sketch found
		val sketchFlow = repository.getDeviceFromId(sketchIdUuid)
		sketchFlow.onEach { res ->
			_isLoading.update { res is Resource.Loading }
			when (res) {
				is Resource.Error -> {
					val message = res.message ?: res.error.message ?: "Some errror"
					_uiEvents.emit(UIEvents.ShowSnackBar(message))
					_isContentLoadFailed.update { true }
				}

				is Resource.Success -> {
					_loadedSketch.update { res.data }
					_screenState.update { state ->
						state.contentTitleState.setTextAndPlaceCursorAtEnd(res.data.title)
						state.contentTextState.setTextAndPlaceCursorAtEnd(res.data.content)
						state.copy(isNewContent = false)
					}
				}

				else -> {}
			}
		}.launchIn(this)
	}

	private fun onUpdateSketch() = viewModelScope.launch {
		val localDeviceId = readLocalDeviceId() ?: return@launch
		val cachedModel = _loadedSketch.value ?: return@launch
		val content = _screenState.value

		if (content.contentTextState.text.isEmpty()) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Content cannot be empty"))
			return@launch
		}
		if (content.contentTitleState.text.isEmpty()) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Content title cannot be empty"))
			return@launch
		}

		val updateModel = cachedModel.copy(
			title = content.contentTitleState.text.toString(),
			content = content.contentTextState.text.toString()
		)
		repository.updateSketch(updateModel, localDeviceId)
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "Some errror"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> _uiEvents.emit(UIEvents.PopScreen)
					else -> {}
				}
			}
			.launchIn(this)
	}

	private fun onSaveSketch() = viewModelScope.launch {
		val localDeviceId = readLocalDeviceId() ?: return@launch
		val content = _screenState.value

		if (content.contentTextState.text.isEmpty()) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Content cannot be empty"))
			return@launch
		}
		if (content.contentTitleState.text.isEmpty()) {
			_uiEvents.emit(UIEvents.ShowSnackBar("Content title cannot be empty"))
			return@launch
		}

		val createModel = CreateSketchModel(
			title = content.contentTitleState.text.toString(),
			content = content.contentTextState.text.toString()
		)
		repository.createSketch(createModel, localDeviceId)
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "Some errror"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> _uiEvents.emit(UIEvents.PopScreen)
					else -> {}
				}
			}
			.launchIn(this)
	}

	private fun onDeleteSketch() = viewModelScope.launch {
		val localDeviceId = readLocalDeviceId() ?: return@launch
		val cachedModel = _loadedSketch.value ?: return@launch

		// show inform about delete
		_uiEvents.emit(UIEvents.ShowToast("Deleting Item"))
		_screenState.update { state -> state.copy(showDeleteDialog = false) }

		//start delete
		repository.revokeSketch(cachedModel, localDeviceId)
			.onEach { res ->
				when (res) {
					is Resource.Error -> {
						val message = res.message ?: res.error.message ?: "Some error"
						_uiEvents.emit(UIEvents.ShowSnackBar(message))
					}

					is Resource.Success -> _uiEvents.emit(UIEvents.PopScreen)
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
}