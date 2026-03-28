package com.trailrunbuddy.app.domain.repository

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.Timer
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfiles(): Flow<List<Profile>>
    suspend fun getProfileWithTimers(profileId: Long): Profile?
    suspend fun saveProfile(profile: Profile, timers: List<Timer>): Long
    suspend fun deleteProfile(profileId: Long)
    suspend fun updateProfileOrder(orderedIds: List<Long>)
}
