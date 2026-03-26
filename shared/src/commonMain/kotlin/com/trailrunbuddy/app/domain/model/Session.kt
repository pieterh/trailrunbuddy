package com.trailrunbuddy.app.domain.model

data class Session(
    val id: Int = 1,
    val profileId: Long,
    val state: SessionState,
    val startedAt: Long,
    val pausedAt: Long? = null,
    val totalPausedMs: Long = 0L,
    val timerStates: List<TimerState> = emptyList()
)

data class TimerState(
    val timerId: Long,
    val cycleCount: Int = 0,
    val firedOnce: Boolean = false
)
