package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.ProfileItem
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerGroup
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

    private val validTimer = Timer(name = "Drink", durationSeconds = 600, timerType = TimerType.REPEATING)
    private val validProfile = Profile(
        name = "Marathon",
        colorHex = "#1E88E5",
        items = listOf(ProfileItem.StandaloneTimer(validTimer))
    )

    @Before
    fun setUp() {
        useCase = SaveProfileUseCase(repository)
        coEvery { repository.saveProfile(any()) } returns 1L
    }

    @Test
    fun `blank name returns NameError`() = runTest {
        val result = useCase(validProfile.copy(name = "  "))
        assertTrue(result is SaveProfileResult.NameError)
    }

    @Test
    fun `empty name returns NameError`() = runTest {
        val result = useCase(validProfile.copy(name = ""))
        assertTrue(result is SaveProfileResult.NameError)
    }

    @Test
    fun `no timers returns TimersError`() = runTest {
        val result = useCase(validProfile.copy(items = emptyList()))
        assertTrue(result is SaveProfileResult.TimersError)
    }

    @Test
    fun `group with no timers returns TimersError`() = runTest {
        val profileWithEmptyGroup = validProfile.copy(
            items = listOf(ProfileItem.Group(TimerGroup()))
        )
        val result = useCase(profileWithEmptyGroup)
        assertTrue(result is SaveProfileResult.TimersError)
    }

    @Test
    fun `valid standalone timer saves and returns Success`() = runTest {
        val result = useCase(validProfile)
        assertTrue(result is SaveProfileResult.Success)
        assertEquals(1L, (result as SaveProfileResult.Success).profileId)
        coVerify { repository.saveProfile(validProfile) }
    }

    @Test
    fun `valid group with timers saves and returns Success`() = runTest {
        val groupProfile = Profile(
            name = "Trail",
            colorHex = "#000",
            items = listOf(
                ProfileItem.Group(
                    TimerGroup(timers = listOf(validTimer))
                )
            )
        )
        val result = useCase(groupProfile)
        assertTrue(result is SaveProfileResult.Success)
    }
}
