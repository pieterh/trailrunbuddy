package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveProfileUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private lateinit var useCase: SaveProfileUseCase

    private val validTimers = listOf(
        Timer(name = "Drink", durationSeconds = 600, timerType = TimerType.REPEATING)
    )
    private val validProfile = Profile(name = "Marathon", colorHex = "#1E88E5")

    @Before
    fun setUp() {
        useCase = SaveProfileUseCase(repository)
        coEvery { repository.saveProfile(any(), any()) } returns 1L
    }

    @Test
    fun `blank name returns NameError`() = runTest {
        val result = useCase(validProfile.copy(name = "  "), validTimers)
        assertTrue(result is SaveProfileResult.NameError)
    }

    @Test
    fun `empty name returns NameError`() = runTest {
        val result = useCase(validProfile.copy(name = ""), validTimers)
        assertTrue(result is SaveProfileResult.NameError)
    }

    @Test
    fun `empty timer list returns TimersError`() = runTest {
        val result = useCase(validProfile, emptyList())
        assertTrue(result is SaveProfileResult.TimersError)
    }

    @Test
    fun `valid input saves and returns Success with profileId`() = runTest {
        val result = useCase(validProfile, validTimers)
        assertTrue(result is SaveProfileResult.Success)
        assertEquals(1L, (result as SaveProfileResult.Success).profileId)
        coVerify { repository.saveProfile(validProfile, validTimers) }
    }
}
