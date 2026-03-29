package com.trailrunbuddy.app.ui.profiledetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.ProfileItem
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.domain.usecase.profile.GetProfileWithTimersUseCase
import com.trailrunbuddy.app.domain.usecase.profile.SaveProfileResult
import com.trailrunbuddy.app.domain.usecase.profile.SaveProfileUseCase
import com.trailrunbuddy.app.domain.usecase.timer.AddTimerUseCase
import com.trailrunbuddy.app.domain.usecase.timer.UpdateTimerUseCase
import com.trailrunbuddy.app.ui.navigation.Screen
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getProfileWithTimersUseCase: GetProfileWithTimersUseCase = mockk()
    private val saveProfileUseCase: SaveProfileUseCase = mockk()
    private val addTimerUseCase = AddTimerUseCase()
    private val updateTimerUseCase = UpdateTimerUseCase()

    private val existingTimer = Timer(id = 1L, name = "Drink", durationSeconds = 600, timerType = TimerType.REPEATING)
    private val existingProfile = Profile(
        id = 5L,
        name = "Trail",
        colorHex = "#43A047",
        items = listOf(ProfileItem.StandaloneTimer(existingTimer))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVmForNewProfile() = ProfileDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Screen.ProfileDetail.ARG_PROFILE_ID to -1L)),
        getProfileWithTimersUseCase = getProfileWithTimersUseCase,
        saveProfileUseCase = saveProfileUseCase,
        addTimerUseCase = addTimerUseCase,
        updateTimerUseCase = updateTimerUseCase
    )

    private fun buildVmForExistingProfile(profileId: Long = 5L): ProfileDetailViewModel {
        coEvery { getProfileWithTimersUseCase(profileId) } returns existingProfile
        return ProfileDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Screen.ProfileDetail.ARG_PROFILE_ID to profileId)),
            getProfileWithTimersUseCase = getProfileWithTimersUseCase,
            saveProfileUseCase = saveProfileUseCase,
            addTimerUseCase = addTimerUseCase,
            updateTimerUseCase = updateTimerUseCase
        )
    }

    private val ProfileDetailUiState.standaloneTimers: List<Timer>
        get() = items.filterIsInstance<ProfileItem.StandaloneTimer>().map { it.timer }

    @Test
    fun `new profile starts with empty state`() = runTest {
        val vm = buildVmForNewProfile()
        val state = vm.uiState.value
        assertEquals("", state.name)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `existing profile loads name and items`() = runTest {
        val vm = buildVmForExistingProfile()
        val state = vm.uiState.value
        assertEquals("Trail", state.name)
        assertEquals(1, state.standaloneTimers.size)
        assertEquals(existingTimer, state.standaloneTimers[0])
    }

    @Test
    fun `onNameChange updates name and clears error`() = runTest {
        val vm = buildVmForNewProfile()
        vm.onNameChange("Marathon")
        assertEquals("Marathon", vm.uiState.value.name)
        assertNull(vm.uiState.value.nameError)
    }

    @Test
    fun `onSaveTimer adds standalone timer to items`() = runTest {
        val vm = buildVmForNewProfile()
        vm.onSaveTimer("Drink", 600, TimerType.REPEATING)
        assertEquals(1, vm.uiState.value.standaloneTimers.size)
        assertEquals("Drink", vm.uiState.value.standaloneTimers[0].name)
    }

    @Test
    fun `onSaveTimer with zero duration shows error`() = runTest {
        val vm = buildVmForNewProfile()
        vm.onSaveTimer("Drink", 0, TimerType.REPEATING)
        assertTrue(vm.uiState.value.items.isEmpty())
        assertNotNull(vm.uiState.value.timersError)
    }

    @Test
    fun `onDeleteTimer removes timer from items`() = runTest {
        val vm = buildVmForExistingProfile()
        val timer = vm.uiState.value.standaloneTimers[0]
        vm.onDeleteTimer(timer)
        assertTrue(vm.uiState.value.standaloneTimers.isEmpty())
    }

    @Test
    fun `onSaveProfile emits SavedSuccessfully on success`() = runTest {
        coEvery { saveProfileUseCase(any()) } returns SaveProfileResult.Success(5L)
        val vm = buildVmForExistingProfile()
        vm.onNameChange("Trail")
        vm.events.test {
            vm.onSaveProfile()
            assertTrue(awaitItem() is ProfileDetailUiEvent.SavedSuccessfully)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSaveProfile with blank name shows name error`() = runTest {
        coEvery { saveProfileUseCase(any()) } returns SaveProfileResult.NameError("name required")
        val vm = buildVmForExistingProfile()
        vm.onNameChange("")
        vm.onSaveProfile()
        assertNotNull(vm.uiState.value.nameError)
    }

    @Test
    fun `onEditTimer populates editingTimer in state`() = runTest {
        val vm = buildVmForExistingProfile()
        val timer = vm.uiState.value.standaloneTimers[0]
        vm.onEditTimer(timer)
        assertEquals(timer, vm.uiState.value.editingTimer)
        assertTrue(vm.uiState.value.showAddTimerDialog)
    }

    @Test
    fun `onSaveTimer in edit mode updates existing timer`() = runTest {
        val vm = buildVmForExistingProfile()
        val timer = vm.uiState.value.standaloneTimers[0]
        vm.onEditTimer(timer)
        vm.onSaveTimer("Updated Drink", 900, TimerType.REPEATING)

        val updatedTimers = vm.uiState.value.standaloneTimers
        assertEquals("Updated Drink", updatedTimers[0].name)
        assertEquals(900, updatedTimers[0].durationSeconds)
        assertEquals(1, updatedTimers.size)
    }

    @Test
    fun `onAddGroup adds group item`() = runTest {
        val vm = buildVmForNewProfile()
        vm.onAddGroup()
        assertTrue(vm.uiState.value.hasGroup)
        assertEquals(1, vm.uiState.value.items.size)
    }

    @Test
    fun `onAddGroup does nothing if group already exists`() = runTest {
        val vm = buildVmForNewProfile()
        vm.onAddGroup()
        vm.onAddGroup() // second call should be no-op
        assertEquals(1, vm.uiState.value.items.filterIsInstance<ProfileItem.Group>().size)
    }

    @Test
    fun `onShowAddTimerToGroup adds timer inside group`() = runTest {
        val vm = buildVmForNewProfile()
        vm.onAddGroup()
        vm.onShowAddTimerToGroup()
        vm.onSaveTimer("Gel", 2700, TimerType.REPEATING)

        val group = vm.uiState.value.items.filterIsInstance<ProfileItem.Group>().first()
        assertEquals(1, group.group.timers.size)
        assertEquals("Gel", group.group.timers[0].name)
    }

    @Test
    fun `onConfirmDeleteGroup removes group from items`() = runTest {
        val vm = buildVmForNewProfile()
        vm.onAddGroup()
        assertTrue(vm.uiState.value.hasGroup)
        vm.onConfirmDeleteGroup()
        assertTrue(!vm.uiState.value.hasGroup)
    }
}
