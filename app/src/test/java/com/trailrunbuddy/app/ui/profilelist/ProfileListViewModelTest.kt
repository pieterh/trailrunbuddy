package com.trailrunbuddy.app.ui.profilelist

import app.cash.turbine.test
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.domain.usecase.profile.DeleteProfileResult
import com.trailrunbuddy.app.domain.usecase.profile.DeleteProfileUseCase
import com.trailrunbuddy.app.domain.usecase.profile.GetProfilesUseCase
import com.trailrunbuddy.app.domain.usecase.profile.ReorderProfilesUseCase
import com.trailrunbuddy.app.domain.usecase.profile.UndoDeleteProfileUseCase
import com.trailrunbuddy.app.platform.service.SessionServiceConnection
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getProfilesUseCase: GetProfilesUseCase = mockk()
    private val deleteProfileUseCase: DeleteProfileUseCase = mockk()
    private val undoDeleteProfileUseCase: UndoDeleteProfileUseCase = mockk()
    private val reorderProfilesUseCase: ReorderProfilesUseCase = mockk(relaxed = true)
    private val sessionServiceConnection: SessionServiceConnection = mockk(relaxed = true)

    private val activeProfileIdFlow = MutableStateFlow<Long?>(null)

    private val sampleTimers = listOf(
        Timer(id = 1L, name = "Drink", durationSeconds = 600, timerType = TimerType.REPEATING)
    )
    private val sampleProfiles = listOf(
        Profile(id = 1L, name = "Trail", colorHex = "#43A047", timers = sampleTimers),
        Profile(id = 2L, name = "Road", colorHex = "#1E88E5", timers = sampleTimers)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionServiceConnection.activeProfileId } returns activeProfileIdFlow
        every { getProfilesUseCase() } returns flowOf(sampleProfiles)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ProfileListViewModel(
        getProfilesUseCase,
        deleteProfileUseCase,
        undoDeleteProfileUseCase,
        reorderProfilesUseCase,
        sessionServiceConnection
    )

    @Test
    fun `profiles are loaded from use case`() = runTest {
        val vm = buildViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(sampleProfiles, state.profiles)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `active session profile id is reflected in state`() = runTest {
        activeProfileIdFlow.value = 1L
        val vm = buildViewModel()
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(1L, state.activeProfileId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteProfile success shows undo snackbar`() = runTest {
        coEvery { deleteProfileUseCase(any()) } returns DeleteProfileResult.Success
        val vm = buildViewModel()

        vm.onDeleteProfile(sampleProfiles[0])

        vm.uiState.test {
            val state = awaitItem()
            assertTrue(state.showUndoSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteProfile blocked shows error message`() = runTest {
        coEvery { deleteProfileUseCase(any()) } returns DeleteProfileResult.BlockedByActiveSession
        val vm = buildViewModel()

        vm.onDeleteProfile(sampleProfiles[0])

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.showUndoSnackbar)
            assertTrue(state.errorMessage != null)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onUndoDelete calls undo use case and hides snackbar`() = runTest {
        coEvery { deleteProfileUseCase(any()) } returns DeleteProfileResult.Success
        coEvery { undoDeleteProfileUseCase(any()) } returns 1L
        val vm = buildViewModel()

        vm.onDeleteProfile(sampleProfiles[0])
        vm.onUndoDelete()

        vm.uiState.test {
            val state = awaitItem()
            assertFalse(state.showUndoSnackbar)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { undoDeleteProfileUseCase(sampleProfiles[0]) }
    }

    @Test
    fun `onStartSession emits NavigateToActiveSession event`() = runTest {
        val vm = buildViewModel()
        vm.events.test {
            vm.onStartSession(1L)
            val event = awaitItem()
            assertTrue(event is ProfileListUiEvent.NavigateToActiveSession)
            assertEquals(1L, (event as ProfileListUiEvent.NavigateToActiveSession).profileId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onErrorDismissed clears error message`() = runTest {
        coEvery { deleteProfileUseCase(any()) } returns DeleteProfileResult.BlockedByActiveSession
        val vm = buildViewModel()

        vm.onDeleteProfile(sampleProfiles[0])
        vm.onErrorDismissed()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
