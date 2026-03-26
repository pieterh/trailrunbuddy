package com.trailrunbuddy.app.data.repository

import androidx.room.withTransaction
import com.trailrunbuddy.app.core.di.IoDispatcher
import com.trailrunbuddy.app.data.local.TrailRunBuddyDatabase
import com.trailrunbuddy.app.data.local.dao.ProfileDao
import com.trailrunbuddy.app.data.local.dao.TimerDao
import com.trailrunbuddy.app.data.local.mapper.toDomain
import com.trailrunbuddy.app.data.local.mapper.toEntity
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val timerDao: TimerDao,
    private val db: TrailRunBuddyDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileRepository {

    override fun getProfiles(): Flow<List<Profile>> =
        profileDao.observeAll()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getProfileWithTimers(profileId: Long): Profile? =
        withContext(ioDispatcher) {
            profileDao.getById(profileId)?.toDomain()
        }

    override suspend fun saveProfile(profile: Profile, timers: List<Timer>): Long =
        withContext(ioDispatcher) {
            db.withTransaction {
                val savedId = if (profile.id == 0L) {
                    profileDao.insert(profile.toEntity())
                } else {
                    profileDao.update(profile.toEntity())
                    profile.id
                }
                timerDao.deleteByProfileId(savedId)
                if (timers.isNotEmpty()) {
                    timerDao.insertAll(
                        timers.mapIndexed { index, timer ->
                            timer.toEntity(profileId = savedId, sortOrder = index)
                        }
                    )
                }
                savedId
            }
        }

    override suspend fun deleteProfile(profileId: Long) =
        withContext(ioDispatcher) {
            profileDao.deleteById(profileId)
        }
}
