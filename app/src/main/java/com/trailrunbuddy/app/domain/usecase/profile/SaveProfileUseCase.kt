package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import javax.inject.Inject

sealed class SaveProfileResult {
    data class Success(val profileId: Long) : SaveProfileResult()
    data class NameError(val message: String) : SaveProfileResult()
    data class TimersError(val message: String) : SaveProfileResult()
}

class SaveProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profile: Profile, timers: List<Timer>): SaveProfileResult {
        if (profile.name.isBlank()) {
            return SaveProfileResult.NameError("Profile name cannot be empty")
        }
        if (timers.isEmpty()) {
            return SaveProfileResult.TimersError("Add at least one timer")
        }
        val profileId = repository.saveProfile(profile, timers)
        return SaveProfileResult.Success(profileId)
    }
}
