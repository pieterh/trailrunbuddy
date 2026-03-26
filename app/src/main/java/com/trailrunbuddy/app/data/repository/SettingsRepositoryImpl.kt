package com.trailrunbuddy.app.data.repository

import com.trailrunbuddy.app.data.settings.SettingsStorage
import com.trailrunbuddy.app.domain.model.ThemeMode
import com.trailrunbuddy.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val storage: SettingsStorage
) : SettingsRepository {

    override fun getThemeMode(): Flow<ThemeMode> = storage.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = storage.setThemeMode(mode)
}
