package com.trailrunbuddy.app.platform.service

import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.platform.timer.TimerCountdownState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStateHolder @Inject constructor() {

    private val _countdownStates = MutableStateFlow<List<TimerCountdownState>>(emptyList())
    val countdownStates: StateFlow<List<TimerCountdownState>> = _countdownStates.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState?>(null)
    val sessionState: StateFlow<SessionState?> = _sessionState.asStateFlow()

    private val _activeProfileId = MutableStateFlow<Long?>(null)
    val activeProfileId: StateFlow<Long?> = _activeProfileId.asStateFlow()

    fun updateCountdownStates(states: List<TimerCountdownState>) {
        _countdownStates.value = states
    }

    fun updateSessionState(state: SessionState?) {
        _sessionState.value = state
    }

    fun updateActiveProfileId(profileId: Long?) {
        _activeProfileId.value = profileId
    }

    fun clear() {
        _countdownStates.value = emptyList()
        _sessionState.value = null
        _activeProfileId.value = null
    }
}
