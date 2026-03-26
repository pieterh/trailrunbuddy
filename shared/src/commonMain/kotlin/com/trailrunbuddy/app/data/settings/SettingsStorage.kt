package com.trailrunbuddy.app.data.settings

import com.trailrunbuddy.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsStorage {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
