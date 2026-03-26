package com.trailrunbuddy.app.domain.usecase.settings

import com.trailrunbuddy.app.domain.model.ThemeMode
import com.trailrunbuddy.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repository.getThemeMode()
}
