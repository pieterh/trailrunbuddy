package com.trailrunbuddy.app.domain.usecase.settings

import com.trailrunbuddy.app.domain.model.ThemeMode
import com.trailrunbuddy.app.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
