package com.trailrunbuddy.app.domain.model

data class Timer(
    val id: Long = 0,
    val profileId: Long = 0,
    val name: String,
    val durationSeconds: Int,
    val timerType: TimerType,
    val sortOrder: Int = 0
)
