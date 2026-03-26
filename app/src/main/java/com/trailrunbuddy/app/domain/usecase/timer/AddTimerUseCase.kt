package com.trailrunbuddy.app.domain.usecase.timer

import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import javax.inject.Inject

sealed class AddTimerResult {
    data class Success(val timer: Timer) : AddTimerResult()
    data class DurationError(val message: String) : AddTimerResult()
    data class NameError(val message: String) : AddTimerResult()
}

class AddTimerUseCase @Inject constructor() {
    operator fun invoke(
        name: String,
        durationSeconds: Int,
        timerType: TimerType,
        sortOrder: Int = 0
    ): AddTimerResult {
        if (name.isBlank()) {
            return AddTimerResult.NameError("Timer name cannot be empty")
        }
        if (durationSeconds <= 0) {
            return AddTimerResult.DurationError("Duration must be greater than 0")
        }
        return AddTimerResult.Success(
            Timer(
                name = name,
                durationSeconds = durationSeconds,
                timerType = timerType,
                sortOrder = sortOrder
            )
        )
    }
}
