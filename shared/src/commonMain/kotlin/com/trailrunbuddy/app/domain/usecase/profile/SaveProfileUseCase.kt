package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.ProfileItem
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
    suspend operator fun invoke(profile: Profile): SaveProfileResult {
        if (profile.name.isBlank()) {
            return SaveProfileResult.NameError("Profile name cannot be empty")
        }
        if (profile.allTimers.isEmpty()) {
            return SaveProfileResult.TimersError("Add at least one timer")
        }
        val group = profile.group
        if (group != null && group.timers.isEmpty()) {
            return SaveProfileResult.TimersError("Add at least one timer to the group")
        }
        val profileId = repository.saveProfile(profile)
        return SaveProfileResult.Success(profileId)
    }
}
