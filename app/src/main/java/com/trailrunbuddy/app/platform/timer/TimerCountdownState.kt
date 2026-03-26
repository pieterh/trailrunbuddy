package com.trailrunbuddy.app.platform.timer

import com.trailrunbuddy.app.domain.model.Timer

data class TimerCountdownState(
    val timer: Timer,
    val remainingMs: Long,
    val cycleCount: Int,
    val isPreWarning: Boolean,
    val isFinished: Boolean  // ONCE timer that has already fired
)
