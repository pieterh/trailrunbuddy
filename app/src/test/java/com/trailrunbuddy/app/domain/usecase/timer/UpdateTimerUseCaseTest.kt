package com.trailrunbuddy.app.domain.usecase.timer

import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateTimerUseCaseTest {

    private lateinit var useCase: UpdateTimerUseCase
    private val original = Timer(id = 1L, name = "Old", durationSeconds = 300, timerType = TimerType.REPEATING)

    @Before
    fun setUp() {
        useCase = UpdateTimerUseCase()
    }

    @Test
    fun `blank name returns NameError`() {
        val result = useCase(original, "  ", 600, TimerType.REPEATING)
        assertTrue(result is UpdateTimerResult.NameError)
    }

    @Test
    fun `zero duration returns DurationError`() {
        val result = useCase(original, "Drink", 0, TimerType.REPEATING)
        assertTrue(result is UpdateTimerResult.DurationError)
    }

    @Test
    fun `valid update returns Success preserving original id and sortOrder`() {
        val result = useCase(original, "Drink", 600, TimerType.ONCE)
        assertTrue(result is UpdateTimerResult.Success)
        val updated = (result as UpdateTimerResult.Success).timer
        assertEquals(1L, updated.id)
        assertEquals("Drink", updated.name)
        assertEquals(600, updated.durationSeconds)
        assertEquals(TimerType.ONCE, updated.timerType)
        assertEquals(original.sortOrder, updated.sortOrder)
    }
}
