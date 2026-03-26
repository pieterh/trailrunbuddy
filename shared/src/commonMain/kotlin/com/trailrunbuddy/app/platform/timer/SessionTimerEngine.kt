package com.trailrunbuddy.app.platform.timer

import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerState
import com.trailrunbuddy.app.domain.model.TimerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TICK_INTERVAL_MS = 100L
private const val PRE_WARNING_THRESHOLD_MS = 10_000L

class SessionTimerEngine(
    private val timers: List<Timer>,
    private val startedAt: Long,
    initialTotalPausedMs: Long = 0L,
    initialTimerStates: List<TimerState> = emptyList(),
    private val onEvent: suspend (TimerEvent) -> Unit,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var totalPausedMs: Long = initialTotalPausedMs
    private var pausedAtMs: Long? = null

    // Per-timer mutable state (cycle count, firedOnce)
    private val mutableTimerStates: MutableMap<Long, TimerState> = initialTimerStates
        .associateBy { it.timerId }
        .toMutableMap()
        .also { map ->
            timers.forEach { t -> map.putIfAbsent(t.id, TimerState(t.id)) }
        }

    // Track which timers have had their pre-warning fired in the current cycle
    private val preWarningFired: MutableSet<Pair<Long, Int>> = mutableSetOf()

    private val _countdownStates = MutableStateFlow<List<TimerCountdownState>>(emptyList())
    val countdownStates: StateFlow<List<TimerCountdownState>> = _countdownStates.asStateFlow()

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            while (isActive) {
                tick()
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    fun pause() {
        if (pausedAtMs == null) {
            pausedAtMs = clock()
        }
    }

    fun resume() {
        pausedAtMs?.let { pausedAt ->
            totalPausedMs += clock() - pausedAt
            pausedAtMs = null
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun getTotalPausedMs(): Long = totalPausedMs

    fun getTimerStates(): List<TimerState> = mutableTimerStates.values.toList()

    private suspend fun tick() {
        val now = pausedAtMs ?: clock()
        val elapsedMs = now - startedAt - totalPausedMs

        val states = timers.map { timer ->
            val durationMs = timer.durationSeconds * 1000L
            val state = mutableTimerStates.getOrPut(timer.id) { TimerState(timer.id) }

            when (timer.timerType) {
                TimerType.REPEATING -> computeRepeating(timer, durationMs, elapsedMs, state)
                TimerType.ONCE -> computeOnce(timer, durationMs, elapsedMs, state)
            }
        }

        _countdownStates.value = states
    }

    private suspend fun computeRepeating(
        timer: Timer,
        durationMs: Long,
        elapsedMs: Long,
        state: TimerState
    ): TimerCountdownState {
        val cycleCount = (elapsedMs / durationMs).toInt()
        val elapsedInCycle = elapsedMs % durationMs
        val remainingMs = durationMs - elapsedInCycle

        // Fire alert when a new cycle completes
        if (cycleCount > state.cycleCount) {
            mutableTimerStates[timer.id] = state.copy(cycleCount = cycleCount)
            preWarningFired.remove(Pair(timer.id, cycleCount))
            onEvent(TimerEvent.Alert(timer.id, timer.name))
        }

        // Fire pre-warning at T-10s of the current cycle
        val key = Pair(timer.id, cycleCount)
        if (remainingMs <= PRE_WARNING_THRESHOLD_MS && remainingMs > (TICK_INTERVAL_MS * 2) && key !in preWarningFired) {
            preWarningFired.add(key)
            onEvent(TimerEvent.PreWarning(timer.id, timer.name))
        }

        return TimerCountdownState(
            timer = timer,
            remainingMs = remainingMs,
            cycleCount = cycleCount,
            isPreWarning = remainingMs <= PRE_WARNING_THRESHOLD_MS,
            isFinished = false
        )
    }

    private suspend fun computeOnce(
        timer: Timer,
        durationMs: Long,
        elapsedMs: Long,
        state: TimerState
    ): TimerCountdownState {
        if (state.firedOnce) {
            return TimerCountdownState(
                timer = timer,
                remainingMs = 0L,
                cycleCount = 1,
                isPreWarning = false,
                isFinished = true
            )
        }

        val remainingMs = (durationMs - elapsedMs).coerceAtLeast(0L)

        // Fire alert when timer expires
        if (remainingMs == 0L) {
            mutableTimerStates[timer.id] = state.copy(firedOnce = true, cycleCount = 1)
            onEvent(TimerEvent.Alert(timer.id, timer.name))
        }

        // Fire pre-warning at T-10s
        val key = Pair(timer.id, 0)
        if (remainingMs <= PRE_WARNING_THRESHOLD_MS && remainingMs > (TICK_INTERVAL_MS * 2) && key !in preWarningFired) {
            preWarningFired.add(key)
            onEvent(TimerEvent.PreWarning(timer.id, timer.name))
        }

        return TimerCountdownState(
            timer = timer,
            remainingMs = remainingMs,
            cycleCount = 0,
            isPreWarning = remainingMs <= PRE_WARNING_THRESHOLD_MS && !state.firedOnce,
            isFinished = false
        )
    }
}
