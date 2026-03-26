package com.trailrunbuddy.app.domain.usecase.profile

import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UndoDeleteProfileUseCaseTest {

    private val repository: ProfileRepository = mockk()
    private lateinit var useCase: UndoDeleteProfileUseCase

    private val timers = listOf(
        Timer(id = 10L, name = "Drink", durationSeconds = 600, timerType = TimerType.REPEATING)
    )
    private val deletedProfile = Profile(id = 5L, name = "Trail", colorHex = "#43A047", timers = timers)

    @Before
    fun setUp() {
        useCase = UndoDeleteProfileUseCase(repository)
        coEvery { repository.saveProfile(any(), any()) } returns 5L
    }

    @Test
    fun `re-inserts profile with id reset to 0`() = runTest {
        val profileSlot = slot<Profile>()
        coEvery { repository.saveProfile(capture(profileSlot), any()) } returns 5L

        useCase(deletedProfile)

        assertEquals(0L, profileSlot.captured.id)
        assertEquals("Trail", profileSlot.captured.name)
    }

    @Test
    fun `re-inserts with the original timers`() = runTest {
        val timersSlot = slot<List<Timer>>()
        coEvery { repository.saveProfile(any(), capture(timersSlot)) } returns 5L

        useCase(deletedProfile)

        assertEquals(timers, timersSlot.captured)
        coVerify { repository.saveProfile(any(), timers) }
    }
}
