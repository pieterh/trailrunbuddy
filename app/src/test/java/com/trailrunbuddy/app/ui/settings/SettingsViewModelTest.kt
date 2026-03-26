package com.trailrunbuddy.app.ui.settings

import app.cash.turbine.test
import com.trailrunbuddy.app.domain.model.ThemeMode
import com.trailrunbuddy.app.domain.usecase.settings.GetThemeUseCase
import com.trailrunbuddy.app.domain.usecase.settings.SetThemeUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getThemeUseCase: GetThemeUseCase = mockk()
    private val setThemeUseCase: SetThemeUseCase = mockk(relaxed = true)
    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getThemeUseCase() } returns themeModeFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SettingsViewModel(getThemeUseCase, setThemeUseCase)

    @Test
    fun `initial state reflects persisted theme`() = runTest {
        themeModeFlow.value = ThemeMode.DARK
        val vm = buildViewModel()
        vm.uiState.test {
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onThemeModeChanged calls set use case`() = runTest {
        val vm = buildViewModel()
        vm.onThemeModeChanged(ThemeMode.LIGHT)
        coVerify { setThemeUseCase(ThemeMode.LIGHT) }
    }

    @Test
    fun `ui state updates when flow emits new value`() = runTest {
        val vm = buildViewModel()
        vm.uiState.test {
            awaitItem() // initial SYSTEM

            themeModeFlow.value = ThemeMode.DARK
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)

            themeModeFlow.value = ThemeMode.LIGHT
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
