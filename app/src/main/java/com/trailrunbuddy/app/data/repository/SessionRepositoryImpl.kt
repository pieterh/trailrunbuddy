package com.trailrunbuddy.app.data.repository

import com.trailrunbuddy.app.core.di.IoDispatcher
import com.trailrunbuddy.app.data.local.dao.SessionDao
import com.trailrunbuddy.app.data.local.mapper.toDomain
import com.trailrunbuddy.app.data.local.mapper.toEntity
import com.trailrunbuddy.app.domain.model.Session
import com.trailrunbuddy.app.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SessionRepository {

    override suspend fun getActiveSession(): Session? =
        withContext(ioDispatcher) {
            sessionDao.get()?.toDomain()
        }

    override suspend fun saveSession(session: Session) =
        withContext(ioDispatcher) {
            sessionDao.save(session.toEntity())
        }

    override suspend fun deleteSession() =
        withContext(ioDispatcher) {
            sessionDao.delete()
        }
}
