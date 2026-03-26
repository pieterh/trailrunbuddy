package com.trailrunbuddy.app.platform.service

import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.platform.timer.TimerCountdownState
import kotlinx.coroutines.flow.StateFlow

interface SessionController {
    val countdownStates: StateFlow<List<TimerCountdownState>>
    val sessionState: StateFlow<SessionState?>
    val activeProfileId: StateFlow<Long?>
    fun startSession(profileId: Long)
    fun pauseSession()
    fun resumeSession()
    fun stopSession()
}
