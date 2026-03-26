package com.trailrunbuddy.app.domain.usecase.session

import com.trailrunbuddy.app.domain.model.Session
import com.trailrunbuddy.app.domain.repository.SessionRepository
import javax.inject.Inject

class GetActiveSessionUseCase @Inject constructor(
    private val repository: SessionRepository
) {
    suspend operator fun invoke(): Session? = repository.getActiveSession()
}
