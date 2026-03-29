package com.trailrunbuddy.app.ui.profiledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailrunbuddy.app.core.util.ColorGenerator
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.ProfileItem
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerGroup
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
                        items = profile.items
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null) }

    // ── Add item menu ─────────────────────────────────────────────────────────

    fun onShowAddItemMenu() = _uiState.update { it.copy(showAddItemMenu = true) }

    fun onDismissAddItemMenu() = _uiState.update { it.copy(showAddItemMenu = false) }

    fun onShowAddStandaloneTimer() = _uiState.update {
        it.copy(showAddItemMenu = false, showAddTimerDialog = true, addingTimerToGroup = false, editingTimer = null)
    }

    fun onShowAddTimerToGroup() = _uiState.update {
        it.copy(showAddItemMenu = false, showAddTimerDialog = true, addingTimerToGroup = true, editingTimer = null)
    }

    fun onAddGroup() {
        val current = _uiState.value
        if (current.hasGroup) return
        val sortOrder = current.items.size
        _uiState.update {
            it.copy(
                showAddItemMenu = false,
                items = it.items + ProfileItem.Group(TimerGroup(sortOrder = sortOrder)),
                timersError = null
            )
        }
    }

    // ── Timer dialog ──────────────────────────────────────────────────────────

    fun onEditTimer(timer: Timer) {
        val isGrouped = _uiState.value.items
            .filterIsInstance<ProfileItem.Group>()
            .any { g -> g.group.timers.any { it === timer } }
        _uiState.update {
            it.copy(showAddTimerDialog = true, editingTimer = timer, editingTimerIsGrouped = isGrouped)
        }
    }

    fun onDismissTimerDialog() = _uiState.update {
        it.copy(showAddTimerDialog = false, editingTimer = null, addingTimerToGroup = false, editingTimerIsGrouped = false)
    }

    fun onDeleteTimer(timer: Timer) {
        _uiState.update { state ->
            val newItems = state.items.mapNotNull { item ->
                when (item) {
                    is ProfileItem.StandaloneTimer ->
                        if (item.timer === timer) null else item
                    is ProfileItem.Group ->
                        ProfileItem.Group(item.group.copy(timers = item.group.timers.filter { it !== timer }))
                }
            }
            state.copy(items = newItems, timersError = null)
        }
    }

    fun onSaveTimer(name: String, durationSeconds: Int, timerType: TimerType) {
        val state = _uiState.value
        val editing = state.editingTimer

        if (editing != null) {
            val result = updateTimerUseCase(editing, name, durationSeconds, timerType)
            when (result) {
                is UpdateTimerResult.Success -> {
                    val newItems = if (state.editingTimerIsGrouped) {
                        state.items.map { item ->
                            if (item is ProfileItem.Group) {
                                ProfileItem.Group(item.group.copy(
                                    timers = item.group.timers.map { if (it === editing) result.timer else it }
                                ))
                            } else item
                        }
                    } else {
                        state.items.map { item ->
                            if (item is ProfileItem.StandaloneTimer && item.timer === editing) {
                                ProfileItem.StandaloneTimer(result.timer)
                            } else item
                        }
                    }
                    _uiState.update {
                        it.copy(items = newItems, showAddTimerDialog = false, editingTimer = null, timersError = null)
                    }
                }
                is UpdateTimerResult.NameError ->
                    _uiState.update { it.copy(timersError = result.message) }
                is UpdateTimerResult.DurationError ->
                    _uiState.update { it.copy(timersError = result.message) }
            }
        } else {
            val sortOrder = if (state.addingTimerToGroup) {
                state.items.filterIsInstance<ProfileItem.Group>().firstOrNull()?.group?.timers?.size ?: 0
            } else {
                state.items.size
            }
            val result = addTimerUseCase(name, durationSeconds, timerType, sortOrder)
            when (result) {
                is AddTimerResult.Success -> {
                    val newItems = if (state.addingTimerToGroup) {
                        state.items.map { item ->
                            if (item is ProfileItem.Group) {
                                ProfileItem.Group(item.group.copy(timers = item.group.timers + result.timer))
                            } else item
                        }
                    } else {
                        state.items + ProfileItem.StandaloneTimer(result.timer)
                    }
                    _uiState.update {
                        it.copy(items = newItems, showAddTimerDialog = false, addingTimerToGroup = false, timersError = null)
                    }
                }
                is AddTimerResult.NameError ->
                    _uiState.update { it.copy(timersError = result.message) }
                is AddTimerResult.DurationError ->
                    _uiState.update { it.copy(timersError = result.message) }
            }
        }
    }

    fun onGroupTimerTypeChange(timerType: TimerType) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item is ProfileItem.Group) ProfileItem.Group(item.group.copy(timerType = timerType))
                    else item
                }
            )
        }
    }

    // ── Group deletion ────────────────────────────────────────────────────────

    fun onRequestDeleteGroup() = _uiState.update { it.copy(showDeleteGroupDialog = true) }

    fun onDismissDeleteGroup() = _uiState.update { it.copy(showDeleteGroupDialog = false) }

    fun onConfirmDeleteGroup() = _uiState.update { state ->
        state.copy(
            items = state.items.filter { it !is ProfileItem.Group },
            showDeleteGroupDialog = false
        )
    }

    // ── Save profile ──────────────────────────────────────────────────────────

    fun onSaveProfile() {
        val state = _uiState.value
        val profile = Profile(
            id = if (profileId == -1L) 0L else profileId,
            name = state.name,
            colorHex = ColorGenerator.fromName(state.name),
            items = state.items
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = saveProfileUseCase(profile)
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
