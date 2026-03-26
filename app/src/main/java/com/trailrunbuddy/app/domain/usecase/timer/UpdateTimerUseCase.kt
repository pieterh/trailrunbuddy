package com.trailrunbuddy.app.domain.usecase.timer

import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import javax.inject.Inject

sealed class UpdateTimerResult {
    data class Success(val timer: Timer) : UpdateTimerResult()
    data class DurationError(val message: String) : UpdateTimerResult()
    data class NameError(val message: String) : UpdateTimerResult()
}

class UpdateTimerUseCase @Inject constructor() {
    operator fun invoke(
        original: Timer,
        name: String,
        durationSeconds: Int,
        timerType: TimerType
    ): UpdateTimerResult {
        if (name.isBlank()) {
            return UpdateTimerResult.NameError("Timer name cannot be empty")
        }
        if (durationSeconds <= 0) {
            return UpdateTimerResult.DurationError("Duration must be greater than 0")
        }
        return UpdateTimerResult.Success(
            original.copy(name = name, durationSeconds = durationSeconds, timerType = timerType)
        )
    }
}
