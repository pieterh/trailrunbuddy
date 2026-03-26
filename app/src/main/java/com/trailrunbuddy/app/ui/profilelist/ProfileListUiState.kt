package com.trailrunbuddy.app.ui.profilelist

import com.trailrunbuddy.app.domain.model.Profile

data class ProfileListUiState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = true,
    val activeProfileId: Long? = null,
    val showUndoSnackbar: Boolean = false,
    val errorMessage: String? = null
)

sealed class ProfileListUiEvent {
    data class NavigateToProfileDetail(val profileId: Long) : ProfileListUiEvent()
    data object NavigateToNewProfile : ProfileListUiEvent()
    data class NavigateToActiveSession(val profileId: Long) : ProfileListUiEvent()
    data object NavigateToSettings : ProfileListUiEvent()
    data class ShowError(val message: String) : ProfileListUiEvent()
}
