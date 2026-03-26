package com.trailrunbuddy.app.ui.activesession

import app.cash.turbine.test
import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.platform.service.SessionServiceConnection
import com.trailrunbuddy.app.platform.timer.TimerCountdownState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveSessionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val sessionServiceConnection: SessionServiceConnection = mockk(relaxed = true)

    private val sessionStateFlow = MutableStateFlow<SessionState?>(SessionState.RUNNING)
    private val countdownFlow = MutableStateFlow<List<TimerCountdownState>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionServiceConnection.sessionState } returns sessionStateFlow
        every { sessionServiceConnection.countdownStates } returns countdownFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ActiveSessionViewModel(sessionServiceConnection)

    @Test
    fun `ui state reflects session state from connection`() = runTest {
        val vm = buildViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.sessionState == SessionState.RUNNING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPauseResume calls pause when running`() = runTest {
        sessionStateFlow.value = SessionState.RUNNING
        val vm = buildViewModel()

        vm.onPauseResume()

        verify { sessionServiceConnection.pauseSession() }
    }

    @Test
    fun `onPauseResume calls resume when paused`() = runTest {
        sessionStateFlow.value = SessionState.PAUSED
        val vm = buildViewModel()

        vm.onPauseResume()

        verify { sessionServiceConnection.resumeSession() }
    }

    @Test
    fun `onStopRequested shows confirmation dialog`() = runTest {
        val vm = buildViewModel()
        vm.onStopRequested()
        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.showStopConfirmDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onStopConfirmed stops service and emits navigation event`() = runTest {
        val vm = buildViewModel()
        vm.onStopRequested()

        vm.events.test {
            vm.onStopConfirmed()
            val event = awaitItem()
            assertTrue(event is ActiveSessionUiEvent.NavigateToProfileList)
            cancelAndIgnoreRemainingEvents()
        }
        verify { sessionServiceConnection.stopSession() }
    }

    @Test
    fun `onStopDismissed hides confirmation dialog`() = runTest {
        val vm = buildViewModel()
        vm.onStopRequested()
        vm.onStopDismissed()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.showStopConfirmDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
