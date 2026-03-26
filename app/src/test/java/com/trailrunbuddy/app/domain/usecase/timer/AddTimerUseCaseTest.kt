package com.trailrunbuddy.app.domain.usecase.timer

import com.trailrunbuddy.app.domain.model.TimerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddTimerUseCaseTest {

    private lateinit var useCase: AddTimerUseCase

    @Before
    fun setUp() {
        useCase = AddTimerUseCase()
    }

    @Test
    fun `zero duration returns DurationError`() {
        val result = useCase("Drink", 0, TimerType.REPEATING)
        assertTrue(result is AddTimerResult.DurationError)
    }

    @Test
    fun `negative duration returns DurationError`() {
        val result = useCase("Drink", -10, TimerType.REPEATING)
        assertTrue(result is AddTimerResult.DurationError)
    }

    @Test
    fun `blank name returns NameError`() {
        val result = useCase("  ", 60, TimerType.REPEATING)
        assertTrue(result is AddTimerResult.NameError)
    }

    @Test
    fun `valid inputs return Success with correct Timer`() {
        val result = useCase("Drink", 600, TimerType.REPEATING, sortOrder = 2)
        assertTrue(result is AddTimerResult.Success)
        val timer = (result as AddTimerResult.Success).timer
        assertEquals("Drink", timer.name)
        assertEquals(600, timer.durationSeconds)
        assertEquals(TimerType.REPEATING, timer.timerType)
        assertEquals(2, timer.sortOrder)
    }

    @Test
    fun `once timer type is preserved`() {
        val result = useCase("Stretch", 300, TimerType.ONCE)
        assertTrue(result is AddTimerResult.Success)
        assertEquals(TimerType.ONCE, (result as AddTimerResult.Success).timer.timerType)
    }
}
