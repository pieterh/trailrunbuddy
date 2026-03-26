package com.trailrunbuddy.app.domain.repository

import com.trailrunbuddy.app.domain.model.Session

interface SessionRepository {
    suspend fun getActiveSession(): Session?
    suspend fun saveSession(session: Session)
    suspend fun deleteSession()
}
