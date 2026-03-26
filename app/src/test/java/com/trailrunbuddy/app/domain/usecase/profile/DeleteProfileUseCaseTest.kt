package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Session
import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import com.trailrunbuddy.app.domain.usecase.session.GetActiveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteProfileUseCaseTest {

    private val profileRepository: ProfileRepository = mockk()
    private val getActiveSessionUseCase: GetActiveSessionUseCase = mockk()
    private lateinit var useCase: DeleteProfileUseCase

    @Before
    fun setUp() {
        useCase = DeleteProfileUseCase(profileRepository, getActiveSessionUseCase)
        coEvery { profileRepository.deleteProfile(any()) } returns Unit
    }

    @Test
    fun `deletes profile when no active session`() = runTest {
        coEvery { getActiveSessionUseCase() } returns null

        val result = useCase(profileId = 1L)

        assertTrue(result is DeleteProfileResult.Success)
        coVerify { profileRepository.deleteProfile(1L) }
    }

    @Test
    fun `blocks delete when active session is for this profile`() = runTest {
        val activeSession = Session(
            profileId = 1L,
            state = SessionState.RUNNING,
            startedAt = 0L
        )
        coEvery { getActiveSessionUseCase() } returns activeSession

        val result = useCase(profileId = 1L)

        assertTrue(result is DeleteProfileResult.BlockedByActiveSession)
        coVerify(exactly = 0) { profileRepository.deleteProfile(any()) }
    }

    @Test
    fun `allows delete when active session is for a different profile`() = runTest {
        val activeSession = Session(
            profileId = 99L,
            state = SessionState.RUNNING,
            startedAt = 0L
        )
        coEvery { getActiveSessionUseCase() } returns activeSession

        val result = useCase(profileId = 1L)

        assertTrue(result is DeleteProfileResult.Success)
        coVerify { profileRepository.deleteProfile(1L) }
    }
}
