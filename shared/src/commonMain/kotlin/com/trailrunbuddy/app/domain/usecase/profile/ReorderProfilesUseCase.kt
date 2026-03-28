package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.repository.ProfileRepository
import javax.inject.Inject

class ReorderProfilesUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(orderedIds: List<Long>) {
        repository.updateProfileOrder(orderedIds)
    }
}
