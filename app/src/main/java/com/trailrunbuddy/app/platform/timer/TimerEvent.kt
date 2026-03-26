package com.trailrunbuddy.app.platform.timer

sealed class TimerEvent {
    data class PreWarning(val timerId: Long, val timerName: String) : TimerEvent()
    data class Alert(val timerId: Long, val timerName: String) : TimerEvent()
}
