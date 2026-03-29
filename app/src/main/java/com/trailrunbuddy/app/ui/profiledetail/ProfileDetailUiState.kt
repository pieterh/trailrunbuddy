package com.trailrunbuddy.app.ui.profiledetail

import com.trailrunbuddy.app.domain.model.ProfileItem
import com.trailrunbuddy.app.domain.model.Timer

data class ProfileDetailUiState(
    val isLoading: Boolean = true,
    val profileId: Long = 0,
    val name: String = "",
    val nameError: String? = null,
    val items: List<ProfileItem> = emptyList(),
    val timersError: String? = null,
    val isSaving: Boolean = false,
    // Add item menu
    val showAddItemMenu: Boolean = false,
    // Timer add/edit dialog
    val showAddTimerDialog: Boolean = false,
    val addingTimerToGroup: Boolean = false,
    val editingTimer: Timer? = null,
    val editingTimerIsGrouped: Boolean = false,
    // Delete group confirmation
    val showDeleteGroupDialog: Boolean = false
) {
    val hasGroup: Boolean get() = items.any { it is ProfileItem.Group }
}

sealed class ProfileDetailUiEvent {
    data object SavedSuccessfully : ProfileDetailUiEvent()
    data object NavigateBack : ProfileDetailUiEvent()
}
