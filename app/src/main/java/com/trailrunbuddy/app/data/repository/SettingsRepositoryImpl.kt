package com.trailrunbuddy.app.data.repository

import com.trailrunbuddy.app.data.datastore.SettingsDataStore
import com.trailrunbuddy.app.domain.model.ThemeMode
import com.trailrunbuddy.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override fun getThemeMode(): Flow<ThemeMode> = dataStore.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode)
}
