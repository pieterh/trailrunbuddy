package com.trailrunbuddy.app.ui.profiledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailrunbuddy.app.core.util.ColorGenerator
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.domain.usecase.profile.GetProfileWithTimersUseCase
import com.trailrunbuddy.app.domain.usecase.profile.SaveProfileResult
import com.trailrunbuddy.app.domain.usecase.profile.SaveProfileUseCase
import com.trailrunbuddy.app.domain.usecase.timer.AddTimerResult
import com.trailrunbuddy.app.domain.usecase.timer.AddTimerUseCase
import com.trailrunbuddy.app.domain.usecase.timer.UpdateTimerResult
import com.trailrunbuddy.app.domain.usecase.timer.UpdateTimerUseCase
import com.trailrunbuddy.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileWithTimersUseCase: GetProfileWithTimersUseCase,
    private val saveProfileUseCase: SaveProfileUseCase,
    private val addTimerUseCase: AddTimerUseCase,
    private val updateTimerUseCase: UpdateTimerUseCase
) : ViewModel() {

    private val profileId: Long =
        savedStateHandle[Screen.ProfileDetail.ARG_PROFILE_ID] ?: -1L

    private val _uiState = MutableStateFlow(ProfileDetailUiState())
    val uiState: StateFlow<ProfileDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileDetailUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (profileId != -1L) {
            loadProfile()
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = getProfileWithTimersUseCase(profileId)
            if (profile != null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        profileId = profile.id,
                        name = profile.name,
                        timers = profile.timers
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null) }

    fun onShowAddTimer() =
        _uiState.update { it.copy(showAddTimerDialog = true, editingTimer = null) }

    fun onEditTimer(timer: Timer) =
        _uiState.update { it.copy(showAddTimerDialog = true, editingTimer = timer) }

    fun onDismissTimerDialog() =
        _uiState.update { it.copy(showAddTimerDialog = false, editingTimer = null) }

    fun onDeleteTimer(timer: Timer) =
        _uiState.update { state ->
            state.copy(timers = state.timers.filter { it !== timer }, timersError = null)
        }

    fun onSaveTimer(name: String, durationSeconds: Int, timerType: TimerType) {
        val editing = _uiState.value.editingTimer
        if (editing != null) {
            val result = updateTimerUseCase(editing, name, durationSeconds, timerType)
            when (result) {
                is UpdateTimerResult.Success -> _uiState.update { state ->
                    state.copy(
                        timers = state.timers.map { if (it === editing) result.timer else it },
                        showAddTimerDialog = false,
                        editingTimer = null,
                        timersError = null
                    )
                }
                is UpdateTimerResult.NameError ->
                    _uiState.update { it.copy(timersError = result.message) }
                is UpdateTimerResult.DurationError ->
                    _uiState.update { it.copy(timersError = result.message) }
            }
        } else {
            val sortOrder = _uiState.value.timers.size
            val result = addTimerUseCase(name, durationSeconds, timerType, sortOrder)
            when (result) {
                is AddTimerResult.Success -> _uiState.update { state ->
                    state.copy(
                        timers = state.timers + result.timer,
                        showAddTimerDialog = false,
                        timersError = null
                    )
                }
                is AddTimerResult.NameError ->
                    _uiState.update { it.copy(timersError = result.message) }
                is AddTimerResult.DurationError ->
                    _uiState.update { it.copy(timersError = result.message) }
            }
        }
    }

    fun onSaveProfile() {
        val state = _uiState.value
        val profile = Profile(
            id = if (profileId == -1L) 0L else profileId,
            name = state.name,
            colorHex = ColorGenerator.fromName(state.name)
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = saveProfileUseCase(profile, state.timers)
            when (result) {
                is SaveProfileResult.Success -> _events.send(ProfileDetailUiEvent.SavedSuccessfully)
                is SaveProfileResult.NameError -> _uiState.update {
                    it.copy(isSaving = false, nameError = result.message)
                }
                is SaveProfileResult.TimersError -> _uiState.update {
                    it.copy(isSaving = false, timersError = result.message)
                }
            }
        }
    }
}
