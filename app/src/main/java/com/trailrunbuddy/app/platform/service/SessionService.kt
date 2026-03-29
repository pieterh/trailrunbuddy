package com.trailrunbuddy.app.platform.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import com.trailrunbuddy.app.domain.model.Session
import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.domain.model.TimerState
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import com.trailrunbuddy.app.domain.repository.SessionRepository
import com.trailrunbuddy.app.platform.audio.AudioPlayer
import com.trailrunbuddy.app.platform.notification.SessionNotificationManager
import com.trailrunbuddy.app.platform.timer.SessionTimerEngine
import com.trailrunbuddy.app.platform.timer.TimerEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class SessionService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_PROFILE_ID = "EXTRA_PROFILE_ID"
        private const val PERSIST_INTERVAL_MS = 5_000L
    }

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var stateHolder: SessionStateHolder
    @Inject lateinit var notificationManager: SessionNotificationManager
    @Inject lateinit var audioManager: AudioPlayer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val persistMutex = Mutex()
    private var engine: SessionTimerEngine? = null
    private var persistJob: Job? = null
    private var notificationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentProfileId: Long = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
                if (profileId != -1L) startNewSession(profileId)
            }
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_STOP -> stopSession()
            null -> restoreSession() // Service restarted by system (START_STICKY)
        }
        return START_STICKY
    }

    private fun startNewSession(profileId: Long) {
        serviceScope.launch {
            val profile = profileRepository.getProfileWithTimers(profileId) ?: return@launch
            val startedAt = System.currentTimeMillis()

            val session = Session(
                profileId = profileId,
                state = SessionState.RUNNING,
                startedAt = startedAt,
                timerStates = profile.allTimers.map { TimerState(it.id) }
            )
            sessionRepository.saveSession(session)
            stateHolder.updateActiveProfileId(profileId)
            stateHolder.updateSessionState(SessionState.RUNNING)
            currentProfileId = profileId

            engine = SessionTimerEngine(
                standaloneTimers = profile.standaloneTimers,
                groupTimers = profile.group?.timers ?: emptyList(),
                groupTimerType = profile.group?.timerType ?: TimerType.REPEATING,
                groupSortOrder = profile.group?.sortOrder ?: 0,
                startedAt = startedAt,
                onEvent = ::handleTimerEvent
            )
            launchEngine(profileId)
        }
    }

    private fun restoreSession() {
        serviceScope.launch {
            val session = sessionRepository.getActiveSession() ?: run {
                stopSelf()
                return@launch
            }
            val profile = profileRepository.getProfileWithTimers(session.profileId) ?: run {
                stopSelf()
                return@launch
            }

            currentProfileId = session.profileId
            stateHolder.updateActiveProfileId(session.profileId)
            stateHolder.updateSessionState(session.state)

            engine = SessionTimerEngine(
                standaloneTimers = profile.standaloneTimers,
                groupTimers = profile.group?.timers ?: emptyList(),
                groupTimerType = profile.group?.timerType ?: TimerType.REPEATING,
                groupSortOrder = profile.group?.sortOrder ?: 0,
                startedAt = session.startedAt,
                initialTotalPausedMs = session.totalPausedMs,
                initialTimerStates = session.timerStates,
                onEvent = ::handleTimerEvent
            )

            if (session.state == SessionState.RUNNING) {
                launchEngine(session.profileId)
            } else {
                // Paused — just show notification, don't tick
                showForegroundNotification(session.profileId, "Paused", 0L)
            }
        }
    }

    private fun pauseSession() {
        engine?.pause()
        stateHolder.updateSessionState(SessionState.PAUSED)
        persistCurrentState(SessionState.PAUSED)
    }

    private fun resumeSession() {
        engine?.resume()
        stateHolder.updateSessionState(SessionState.RUNNING)
        persistCurrentState(SessionState.RUNNING)
        if (notificationJob == null && currentProfileId != -1L) {
            launchEngine(currentProfileId)
        }
    }

    private fun stopSession() {
        engine?.stop()
        persistJob?.cancel()
        notificationJob?.cancel()
        serviceScope.launch {
            persistMutex.withLock {
                sessionRepository.deleteSession()
            }
            stateHolder.clear()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun launchEngine(profileId: Long) {
        acquireWakeLock()
        engine?.start(serviceScope)

        var lastNotifiedSecond = -1L
        notificationJob = serviceScope.launch {
            engine?.countdownStates?.collectLatest { states ->
                stateHolder.updateCountdownStates(states)
                // Only show active timers in notification (skip waiting group timers)
                val next = states
                    .filter { !it.isFinished && (!it.isInGroup || it.isActiveInGroup) }
                    .minByOrNull { it.remainingMs }
                if (next != null) {
                    val currentSecond = next.remainingMs / 1000
                    if (currentSecond != lastNotifiedSecond) {
                        lastNotifiedSecond = currentSecond
                        notificationManager.update(profileId, next.timer.name, next.remainingMs)
                    }
                }
            }
        }

        persistJob = serviceScope.launch {
            while (true) {
                delay(PERSIST_INTERVAL_MS)
                persistCurrentState(SessionState.RUNNING)
            }
        }

        showForegroundNotification(profileId, "Starting…", 0L)
    }

    private fun showForegroundNotification(profileId: Long, timerName: String, remainingMs: Long) {
        val notification = notificationManager.buildNotification(profileId, timerName, remainingMs)
        startForeground(
            SessionNotificationManager.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    private fun persistCurrentState(state: SessionState) {
        val currentEngine = engine ?: return
        serviceScope.launch {
            persistMutex.withLock {
                val current = sessionRepository.getActiveSession() ?: return@withLock
                sessionRepository.saveSession(
                    current.copy(
                        state = state,
                        totalPausedMs = currentEngine.getTotalPausedMs(),
                        timerStates = currentEngine.getTimerStates()
                    )
                )
            }
        }
    }

    private suspend fun handleTimerEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.Alert -> {
                audioManager.playAlert()
                stateHolder.notifyAlertFired(event.timerId)
            }
            is TimerEvent.PreWarning -> audioManager.playPreWarning()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrailRunBuddy:SessionWakeLock")
        wakeLock?.acquire(12 * 60 * 60 * 1000L) // 12 hours max
    }

    override fun onDestroy() {
        engine?.stop()
        serviceScope.cancel()
        wakeLock?.release()
        super.onDestroy()
    }
}
