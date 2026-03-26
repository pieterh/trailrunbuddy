package com.trailrunbuddy.app.ui.activesession

import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.platform.timer.TimerCountdownState

data class ActiveSessionUiState(
    val isLoading: Boolean = true,
    val sessionState: SessionState? = null,
    val countdownStates: List<TimerCountdownState> = emptyList(),
    val showStopConfirmDialog: Boolean = false
)

sealed class ActiveSessionUiEvent {
    data object NavigateToProfileList : ActiveSessionUiEvent()
}
