package com.trailrunbuddy.app.platform.timer

import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerState
import com.trailrunbuddy.app.domain.model.TimerType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionTimerEngineTest {

    private var currentTime = 0L
    private val clock = { currentTime }

    private fun repeatingTimer(durationSeconds: Int = 60) = Timer(
        id = 1L, name = "Drink", durationSeconds = durationSeconds, timerType = TimerType.REPEATING
    )

    private fun onceTimer(durationSeconds: Int = 60) = Timer(
        id = 1L, name = "Stretch", durationSeconds = durationSeconds, timerType = TimerType.ONCE
    )

    @Test
    fun `repeating timer shows full duration at start`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 0L
        val engine = SessionTimerEngine(
            timers = listOf(repeatingTimer(60)),
            startedAt = 0L,
            onEvent = {},
            clock = clock
        )
        engine.start(this)
        advanceTimeBy(100)

        val states = engine.countdownStates.value
        assertEquals(1, states.size)
        assertEquals(60_000L, states[0].remainingMs)
        assertEquals(0, states[0].cycleCount)
        assertFalse(states[0].isFinished)

        engine.stop()
    }

    @Test
    fun `repeating timer counts down correctly at mid-point`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 30_000L
        val engine = SessionTimerEngine(
            timers = listOf(repeatingTimer(60)),
            startedAt = 0L,
            onEvent = {},
            clock = clock
        )
        engine.start(this)
        advanceTimeBy(100)

        val states = engine.countdownStates.value
        assertEquals(30_000L, states[0].remainingMs)
        assertEquals(0, states[0].cycleCount)

        engine.stop()
    }

    @Test
    fun `repeating timer fires Alert and increments cycle at boundary`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = mutableListOf<TimerEvent>()
            currentTime = 60_100L // just past first cycle
            val engine = SessionTimerEngine(
                timers = listOf(repeatingTimer(60)),
                startedAt = 0L,
                onEvent = { events.add(it) },
                clock = clock
            )
            engine.start(this)
            advanceTimeBy(100)

            val states = engine.countdownStates.value
            assertEquals(1, states[0].cycleCount)
            assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 1L })

            engine.stop()
        }

    @Test
    fun `repeating timer shows pre-warning at T minus 10s`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = mutableListOf<TimerEvent>()
            currentTime = 55_000L // 5 seconds in, 55s remaining... no wait: 60s total, 55s elapsed → 5s remaining
            val engine = SessionTimerEngine(
                timers = listOf(repeatingTimer(60)),
                startedAt = 0L,
                onEvent = { events.add(it) },
                clock = clock
            )
            engine.start(this)
            advanceTimeBy(100)

            val states = engine.countdownStates.value
            assertEquals(5_000L, states[0].remainingMs)
            assertTrue(states[0].isPreWarning)
            assertTrue(events.any { it is TimerEvent.PreWarning && it.timerId == 1L })

            engine.stop()
        }

    @Test
    fun `once timer fires Alert and marks as finished`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = mutableListOf<TimerEvent>()
            currentTime = 60_000L // exactly at expiry
            val engine = SessionTimerEngine(
                timers = listOf(onceTimer(60)),
                startedAt = 0L,
                onEvent = { events.add(it) },
                clock = clock
            )
            engine.start(this)
            advanceTimeBy(100)

            val states = engine.countdownStates.value
            assertEquals(0L, states[0].remainingMs)
            assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 1L })

            engine.stop()
        }

    @Test
    fun `once timer is finished after firing`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 90_000L // past expiry
        val initialState = listOf(TimerState(timerId = 1L, cycleCount = 1, firedOnce = true))
        val engine = SessionTimerEngine(
            timers = listOf(onceTimer(60)),
            startedAt = 0L,
            initialTimerStates = initialState,
            onEvent = {},
            clock = clock
        )
        engine.start(this)
        advanceTimeBy(100)

        val states = engine.countdownStates.value
        assertTrue(states[0].isFinished)
        assertEquals(0L, states[0].remainingMs)

        engine.stop()
    }

    @Test
    fun `pause stops elapsed time from advancing`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 10_000L
        val engine = SessionTimerEngine(
            timers = listOf(repeatingTimer(60)),
            startedAt = 0L,
            onEvent = {},
            clock = clock
        )
        engine.start(this)
        advanceTimeBy(100)

        engine.pause()
        val pausedAt = currentTime

        // Move time forward while paused — remaining should not change
        currentTime = 30_000L
        advanceTimeBy(100)
        val statesDuringPause = engine.countdownStates.value

        engine.resume()
        // After resume, elapsed = (now - start) - totalPaused
        // = 30000 - (30000 - pausedAt) = pausedAt = 10000 → remaining = 50000
        currentTime = 30_000L
        advanceTimeBy(100)

        // The paused snapshot should still show 50s remaining (paused at 10s)
        assertEquals(50_000L, statesDuringPause[0].remainingMs)

        engine.stop()
    }

    @Test
    fun `getTimerStates returns current state`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 0L
        val engine = SessionTimerEngine(
            timers = listOf(repeatingTimer(60)),
            startedAt = 0L,
            onEvent = {},
            clock = clock
        )
        engine.start(this)
        advanceTimeBy(100)

        val states = engine.getTimerStates()
        assertEquals(1, states.size)
        assertEquals(1L, states[0].timerId)

        engine.stop()
    }

    @Test
    fun `multiple timers emit independent states`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 30_000L
        val timers = listOf(
            Timer(id = 1L, name = "Drink", durationSeconds = 60, timerType = TimerType.REPEATING),
            Timer(id = 2L, name = "Eat", durationSeconds = 120, timerType = TimerType.REPEATING)
        )
        val engine = SessionTimerEngine(
            timers = timers,
            startedAt = 0L,
            onEvent = {},
            clock = clock
        )
        engine.start(this)
        advanceTimeBy(100)

        val states = engine.countdownStates.value
        assertEquals(2, states.size)

        val drink = states.first { it.timer.id == 1L }
        val eat = states.first { it.timer.id == 2L }
        assertEquals(30_000L, drink.remainingMs)
        assertEquals(90_000L, eat.remainingMs)

        engine.stop()
    }
}
