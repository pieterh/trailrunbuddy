package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfilesUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(): Flow<List<Profile>> = repository.getProfiles()
}
