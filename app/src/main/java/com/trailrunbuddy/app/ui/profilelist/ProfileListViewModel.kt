package com.trailrunbuddy.app.ui.profilelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.usecase.profile.DeleteProfileResult
import com.trailrunbuddy.app.domain.usecase.profile.DeleteProfileUseCase
import com.trailrunbuddy.app.domain.usecase.profile.GetProfilesUseCase
import com.trailrunbuddy.app.domain.usecase.profile.ReorderProfilesUseCase
import com.trailrunbuddy.app.domain.usecase.profile.UndoDeleteProfileUseCase
import com.trailrunbuddy.app.platform.service.SessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val undoDeleteProfileUseCase: UndoDeleteProfileUseCase,
    private val reorderProfilesUseCase: ReorderProfilesUseCase,
    private val sessionController: SessionController
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileListUiState())
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileListUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var deletedProfile: Profile? = null

    init {
        combine(
            getProfilesUseCase(),
            sessionController.activeProfileId
        ) { profiles, activeId ->
            _uiState.update { state ->
                state.copy(
                    profiles = profiles,
                    isLoading = false,
                    activeProfileId = activeId
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onDeleteProfile(profile: Profile) {
        viewModelScope.launch {
            val result = deleteProfileUseCase(profile.id)
            when (result) {
                is DeleteProfileResult.Success -> {
                    deletedProfile = profile
                    _uiState.update { it.copy(showUndoSnackbar = true) }
                }
                is DeleteProfileResult.BlockedByActiveSession -> {
                    _uiState.update { it.copy(errorMessage = "Cannot delete a profile with an active session") }
                }
            }
        }
    }

    fun onUndoDelete() {
        val profile = deletedProfile ?: return
        viewModelScope.launch {
            undoDeleteProfileUseCase(profile)
            deletedProfile = null
            _uiState.update { it.copy(showUndoSnackbar = false) }
        }
    }

    fun onUndoSnackbarDismissed() {
        deletedProfile = null
        _uiState.update { it.copy(showUndoSnackbar = false) }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(errorMessage = null) }

    fun onReorder(from: Int, to: Int) {
        _uiState.update { state ->
            val reordered = state.profiles.toMutableList().apply { add(to, removeAt(from)) }
            state.copy(profiles = reordered)
        }
    }

    fun onReorderFinished() {
        viewModelScope.launch {
            try {
                reorderProfilesUseCase(_uiState.value.profiles.map { it.id })
            } catch (e: Exception) {
                _events.send(ProfileListUiEvent.ShowError("Failed to save new order"))
            }
        }
    }

    fun onStartSession(profileId: Long) {
        sessionController.startSession(profileId)
        viewModelScope.launch {
            _events.send(ProfileListUiEvent.NavigateToActiveSession(profileId))
        }
    }
}
