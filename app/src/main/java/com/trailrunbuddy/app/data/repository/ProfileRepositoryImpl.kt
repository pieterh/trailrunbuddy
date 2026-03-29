package com.trailrunbuddy.app.data.repository

import androidx.room.withTransaction
import com.trailrunbuddy.app.core.di.IoDispatcher
import com.trailrunbuddy.app.data.local.TrailRunBuddyDatabase
import com.trailrunbuddy.app.data.local.dao.ProfileDao
import com.trailrunbuddy.app.data.local.dao.TimerDao
import com.trailrunbuddy.app.data.local.dao.TimerGroupDao
import com.trailrunbuddy.app.data.local.entity.TimerGroupEntity
import com.trailrunbuddy.app.data.local.mapper.toDomain
import com.trailrunbuddy.app.data.local.mapper.toEntity
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.ProfileItem
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
    private val timerGroupDao: TimerGroupDao,
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

    override suspend fun saveProfile(profile: Profile): Long =
        withContext(ioDispatcher) {
            db.withTransaction {
                val savedId = if (profile.id == 0L) {
                    profileDao.insert(profile.toEntity())
                } else {
                    profileDao.update(profile.toEntity())
                    profile.id
                }

                // Clear all existing timers and groups for this profile
                timerDao.deleteByProfileId(savedId)
                timerGroupDao.deleteByProfileId(savedId)

                // Re-insert items in order
                val timerEntities = mutableListOf<com.trailrunbuddy.app.data.local.entity.TimerEntity>()
                profile.items.forEachIndexed { itemIndex, item ->
                    when (item) {
                        is ProfileItem.StandaloneTimer -> {
                            timerEntities.add(
                                item.timer.toEntity(profileId = savedId, sortOrder = itemIndex, groupId = null)
                            )
                        }
                        is ProfileItem.Group -> {
                            val groupId = timerGroupDao.insert(
                                TimerGroupEntity(profileId = savedId, sortOrder = itemIndex)
                            )
                            item.group.timers.forEachIndexed { timerIndex, timer ->
                                timerEntities.add(
                                    timer.toEntity(profileId = savedId, sortOrder = timerIndex, groupId = groupId)
                                )
                            }
                        }
                    }
                }

                if (timerEntities.isNotEmpty()) {
                    timerDao.insertAll(timerEntities)
                }

                savedId
            }
        }

    override suspend fun deleteProfile(profileId: Long) =
        withContext(ioDispatcher) {
            profileDao.deleteById(profileId)
        }

    override suspend fun updateProfileOrder(orderedIds: List<Long>) =
        withContext(ioDispatcher) {
            db.withTransaction {
                val updated = orderedIds.mapIndexedNotNull { index, id ->
                    profileDao.getById(id)?.profile?.copy(sortOrder = index)
                }
                profileDao.updateAll(updated)
            }
        }
}
