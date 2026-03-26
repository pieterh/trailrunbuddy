package com.trailrunbuddy.app.ui.settings

import com.trailrunbuddy.app.domain.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
