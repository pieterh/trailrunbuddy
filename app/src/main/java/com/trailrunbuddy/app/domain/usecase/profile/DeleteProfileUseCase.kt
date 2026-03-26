package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.repository.ProfileRepository
import com.trailrunbuddy.app.domain.usecase.session.GetActiveSessionUseCase
import javax.inject.Inject

sealed class DeleteProfileResult {
    data object Success : DeleteProfileResult()
    data object BlockedByActiveSession : DeleteProfileResult()
}

class DeleteProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val getActiveSessionUseCase: GetActiveSessionUseCase
) {
    suspend operator fun invoke(profileId: Long): DeleteProfileResult {
        val activeSession = getActiveSessionUseCase()
        if (activeSession?.profileId == profileId) {
            return DeleteProfileResult.BlockedByActiveSession
        }
        profileRepository.deleteProfile(profileId)
        return DeleteProfileResult.Success
    }
}
