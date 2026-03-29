package com.trailrunbuddy.app.domain.repository

import com.trailrunbuddy.app.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfiles(): Flow<List<Profile>>
    suspend fun getProfileWithTimers(profileId: Long): Profile?
    suspend fun saveProfile(profile: Profile): Long
    suspend fun deleteProfile(profileId: Long)
    suspend fun updateProfileOrder(orderedIds: List<Long>)
}
