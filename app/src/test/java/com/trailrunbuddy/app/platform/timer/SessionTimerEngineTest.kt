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

    private fun repeatingTimer(durationSeconds: Int = 60, id: Long = 1L) = Timer(
        id = id, name = "Drink", durationSeconds = durationSeconds, timerType = TimerType.REPEATING
    )

    private fun onceTimer(durationSeconds: Int = 60) = Timer(
        id = 1L, name = "Stretch", durationSeconds = durationSeconds, timerType = TimerType.ONCE
    )

    private fun engine(
        standaloneTimers: List<Timer> = emptyList(),
        groupTimers: List<Timer> = emptyList(),
        groupTimerType: TimerType = TimerType.REPEATING,
        startedAt: Long = 0L,
        initialTimerStates: List<TimerState> = emptyList(),
        onEvent: suspend (TimerEvent) -> Unit = {}
    ) = SessionTimerEngine(
        standaloneTimers = standaloneTimers,
        groupTimers = groupTimers,
        groupTimerType = groupTimerType,
        startedAt = startedAt,
        initialTimerStates = initialTimerStates,
        onEvent = onEvent,
        clock = clock
    )

    @Test
    fun `repeating timer shows full duration at start`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 0L
        val e = engine(standaloneTimers = listOf(repeatingTimer(60)))
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        assertEquals(1, states.size)
        assertEquals(60_000L, states[0].remainingMs)
        assertEquals(0, states[0].cycleCount)
        assertFalse(states[0].isFinished)

        e.stop()
    }

    @Test
    fun `repeating timer counts down correctly at mid-point`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 30_000L
        val e = engine(standaloneTimers = listOf(repeatingTimer(60)))
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        assertEquals(30_000L, states[0].remainingMs)
        assertEquals(0, states[0].cycleCount)

        e.stop()
    }

    @Test
    fun `repeating timer fires Alert and increments cycle at boundary`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = mutableListOf<TimerEvent>()
            currentTime = 60_100L
            val e = engine(standaloneTimers = listOf(repeatingTimer(60)), onEvent = { events.add(it) })
            e.start(this)
            advanceTimeBy(100)

            val states = e.countdownStates.value
            assertEquals(1, states[0].cycleCount)
            assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 1L })

            e.stop()
        }

    @Test
    fun `repeating timer shows pre-warning at T minus 10s`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = mutableListOf<TimerEvent>()
            currentTime = 55_000L
            val e = engine(standaloneTimers = listOf(repeatingTimer(60)), onEvent = { events.add(it) })
            e.start(this)
            advanceTimeBy(100)

            val states = e.countdownStates.value
            assertEquals(5_000L, states[0].remainingMs)
            assertTrue(states[0].isPreWarning)
            assertTrue(events.any { it is TimerEvent.PreWarning && it.timerId == 1L })

            e.stop()
        }

    @Test
    fun `once timer fires Alert and marks as finished`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = mutableListOf<TimerEvent>()
            currentTime = 60_000L
            val e = engine(standaloneTimers = listOf(onceTimer(60)), onEvent = { events.add(it) })
            e.start(this)
            advanceTimeBy(100)

            val states = e.countdownStates.value
            assertEquals(0L, states[0].remainingMs)
            assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 1L })

            e.stop()
        }

    @Test
    fun `once timer is finished after firing`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 90_000L
        val initialState = listOf(TimerState(timerId = 1L, cycleCount = 1, firedOnce = true))
        val e = engine(
            standaloneTimers = listOf(onceTimer(60)),
            initialTimerStates = initialState
        )
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        assertTrue(states[0].isFinished)
        assertEquals(0L, states[0].remainingMs)

        e.stop()
    }

    @Test
    fun `pause stops elapsed time from advancing`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 10_000L
        val e = engine(standaloneTimers = listOf(repeatingTimer(60)))
        e.start(this)
        advanceTimeBy(100)

        e.pause()

        currentTime = 30_000L
        advanceTimeBy(100)
        val statesDuringPause = e.countdownStates.value

        e.resume()
        currentTime = 30_000L
        advanceTimeBy(100)

        assertEquals(50_000L, statesDuringPause[0].remainingMs)

        e.stop()
    }

    @Test
    fun `getTimerStates returns current state`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 0L
        val e = engine(standaloneTimers = listOf(repeatingTimer(60)))
        e.start(this)
        advanceTimeBy(100)

        val states = e.getTimerStates()
        assertEquals(1, states.size)
        assertEquals(1L, states[0].timerId)

        e.stop()
    }

    @Test
    fun `multiple standalone timers emit independent states`() = runTest(UnconfinedTestDispatcher()) {
        currentTime = 30_000L
        val timers = listOf(
            Timer(id = 1L, name = "Drink", durationSeconds = 60, timerType = TimerType.REPEATING),
            Timer(id = 2L, name = "Eat", durationSeconds = 120, timerType = TimerType.REPEATING)
        )
        val e = engine(standaloneTimers = timers)
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        assertEquals(2, states.size)
        assertEquals(30_000L, states.first { it.timer.id == 1L }.remainingMs)
        assertEquals(90_000L, states.first { it.timer.id == 2L }.remainingMs)

        e.stop()
    }

    @Test
    fun `group timers fire sequentially round-robin`() = runTest(UnconfinedTestDispatcher()) {
        // Group: A(20s), B(45s) — total cycle = 65s
        val timerA = Timer(id = 10L, name = "A", durationSeconds = 20, timerType = TimerType.REPEATING)
        val timerB = Timer(id = 11L, name = "B", durationSeconds = 45, timerType = TimerType.REPEATING)
        val events = mutableListOf<TimerEvent>()

        // At t=21s: A has fired (at 20s), B is now active with 44s remaining
        currentTime = 21_000L
        val e = engine(groupTimers = listOf(timerA, timerB), onEvent = { events.add(it) })
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        assertEquals(2, states.size)

        val stateA = states.first { it.timer.id == 10L }
        val stateB = states.first { it.timer.id == 11L }

        // A has fired once and is waiting (shows full duration)
        assertFalse(stateA.isActiveInGroup)
        assertEquals(1, stateA.cycleCount)
        assertTrue(stateA.isInGroup)

        // B is active with 44s remaining
        assertTrue(stateB.isActiveInGroup)
        assertEquals(44_000L, stateB.remainingMs)

        // Alert fired for A
        assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 10L })

        e.stop()
    }

    @Test
    fun `group timer wraps around after full cycle`() = runTest(UnconfinedTestDispatcher()) {
        // Group: A(20s), B(45s) — cycle = 65s
        // At t=66s: second cycle started (1s in), A is active again
        val timerA = Timer(id = 10L, name = "A", durationSeconds = 20, timerType = TimerType.REPEATING)
        val timerB = Timer(id = 11L, name = "B", durationSeconds = 45, timerType = TimerType.REPEATING)
        val events = mutableListOf<TimerEvent>()

        currentTime = 66_000L
        val e = engine(groupTimers = listOf(timerA, timerB), onEvent = { events.add(it) })
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        val stateA = states.first { it.timer.id == 10L }
        val stateB = states.first { it.timer.id == 11L }

        // A is active again in second cycle
        assertTrue(stateA.isActiveInGroup)
        // B fired at end of cycle 1
        assertFalse(stateB.isActiveInGroup)

        // Both A and B fired at least once
        assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 10L })
        assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 11L })

        e.stop()
    }

    @Test
    fun `standalone and group timers coexist independently`() = runTest(UnconfinedTestDispatcher()) {
        val standalone = Timer(id = 1L, name = "Stretch", durationSeconds = 30, timerType = TimerType.REPEATING)
        val groupA = Timer(id = 2L, name = "Drink", durationSeconds = 20, timerType = TimerType.REPEATING)
        val groupB = Timer(id = 3L, name = "Gel", durationSeconds = 45, timerType = TimerType.REPEATING)

        currentTime = 15_000L // 15s in: standalone halfway, groupA active
        val e = engine(standaloneTimers = listOf(standalone), groupTimers = listOf(groupA, groupB))
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        assertEquals(3, states.size)

        val stretchState = states.first { it.timer.id == 1L }
        val drinkState = states.first { it.timer.id == 2L }
        val gelState = states.first { it.timer.id == 3L }

        // Standalone: 15s remaining
        assertFalse(stretchState.isInGroup)
        assertEquals(15_000L, stretchState.remainingMs)

        // Drink: active in group, 5s remaining
        assertTrue(drinkState.isInGroup)
        assertTrue(drinkState.isActiveInGroup)
        assertEquals(5_000L, drinkState.remainingMs)

        // Gel: waiting in group
        assertTrue(gelState.isInGroup)
        assertFalse(gelState.isActiveInGroup)

        e.stop()
    }

    @Test
    fun `once group marks all timers finished after full cycle`() = runTest(UnconfinedTestDispatcher()) {
        // Group: A(20s), B(45s) — cycle = 65s
        val timerA = Timer(id = 10L, name = "A", durationSeconds = 20, timerType = TimerType.REPEATING)
        val timerB = Timer(id = 11L, name = "B", durationSeconds = 45, timerType = TimerType.REPEATING)
        val events = mutableListOf<TimerEvent>()

        // At t=66s: full cycle completed, group should be done
        currentTime = 66_000L
        val e = engine(
            groupTimers = listOf(timerA, timerB),
            groupTimerType = TimerType.ONCE,
            onEvent = { events.add(it) }
        )
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        val stateA = states.first { it.timer.id == 10L }
        val stateB = states.first { it.timer.id == 11L }

        assertTrue(stateA.isFinished)
        assertTrue(stateB.isFinished)
        assertFalse(stateA.isActiveInGroup)
        assertFalse(stateB.isActiveInGroup)
        assertEquals(0L, stateA.remainingMs)
        assertEquals(0L, stateB.remainingMs)

        // Both timers fired their alerts
        assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 10L })
        assertTrue(events.any { it is TimerEvent.Alert && it.timerId == 11L })

        e.stop()
    }

    @Test
    fun `once group stays active before cycle completes`() = runTest(UnconfinedTestDispatcher()) {
        // Group: A(20s), B(45s) — cycle = 65s. At t=21s: still in first cycle
        val timerA = Timer(id = 10L, name = "A", durationSeconds = 20, timerType = TimerType.REPEATING)
        val timerB = Timer(id = 11L, name = "B", durationSeconds = 45, timerType = TimerType.REPEATING)

        currentTime = 21_000L
        val e = engine(
            groupTimers = listOf(timerA, timerB),
            groupTimerType = TimerType.ONCE
        )
        e.start(this)
        advanceTimeBy(100)

        val states = e.countdownStates.value
        val stateB = states.first { it.timer.id == 11L }

        // Mid-cycle: B should still be active, not finished
        assertFalse(stateB.isFinished)
        assertTrue(stateB.isActiveInGroup)

        e.stop()
    }
}
