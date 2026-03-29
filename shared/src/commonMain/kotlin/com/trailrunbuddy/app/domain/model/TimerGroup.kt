package com.trailrunbuddy.app.domain.model

data class TimerGroup(
    val id: Long = 0,
    val profileId: Long = 0,
    val sortOrder: Int = 0,
    val timerType: TimerType = TimerType.REPEATING,
    val timers: List<Timer> = emptyList()
)
