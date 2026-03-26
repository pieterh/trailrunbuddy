package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import javax.inject.Inject

class GetProfileWithTimersUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profileId: Long): Profile? =
        repository.getProfileWithTimers(profileId)
}
