package com.trailrunbuddy.app.domain.usecase.session

import com.trailrunbuddy.app.domain.repository.SessionRepository
import javax.inject.Inject

class StopSessionUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke() = repository.deleteSession()
}
