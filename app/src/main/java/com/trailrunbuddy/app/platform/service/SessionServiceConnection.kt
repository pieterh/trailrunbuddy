package com.trailrunbuddy.app.platform.service

import android.content.Context
import android.content.Intent
import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.platform.timer.TimerCountdownState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateHolder: SessionStateHolder
) {
    val countdownStates: StateFlow<List<TimerCountdownState>> = stateHolder.countdownStates
    val sessionState: StateFlow<SessionState?> = stateHolder.sessionState
    val activeProfileId: StateFlow<Long?> = stateHolder.activeProfileId

    fun startSession(profileId: Long) {
        val intent = Intent(context, SessionService::class.java).apply {
            action = SessionService.ACTION_START
            putExtra(SessionService.EXTRA_PROFILE_ID, profileId)
        }
        context.startForegroundService(intent)
    }

    fun pauseSession() = sendAction(SessionService.ACTION_PAUSE)

    fun resumeSession() = sendAction(SessionService.ACTION_RESUME)

    fun stopSession() = sendAction(SessionService.ACTION_STOP)

    private fun sendAction(action: String) {
        val intent = Intent(context, SessionService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }
}
