package com.trailrunbuddy.app.ui.profiledetail

import com.trailrunbuddy.app.domain.model.Timer

data class ProfileDetailUiState(
    val isLoading: Boolean = true,
    val profileId: Long = 0,
    val name: String = "",
    val nameError: String? = null,
    val timers: List<Timer> = emptyList(),
    val timersError: String? = null,
    val isSaving: Boolean = false,
    val showAddTimerDialog: Boolean = false,
    val editingTimer: Timer? = null   // non-null when editing an existing timer
)

sealed class ProfileDetailUiEvent {
    data object SavedSuccessfully : ProfileDetailUiEvent()
    data object NavigateBack : ProfileDetailUiEvent()
}
