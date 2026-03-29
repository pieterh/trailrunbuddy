package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import javax.inject.Inject

class UndoDeleteProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profile: Profile): Long =
        repository.saveProfile(profile.copy(id = 0))
}
