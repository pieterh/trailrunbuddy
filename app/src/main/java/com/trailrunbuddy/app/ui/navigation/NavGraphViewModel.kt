package com.trailrunbuddy.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.trailrunbuddy.app.platform.service.SessionServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavGraphViewModel @Inject constructor(
    val sessionServiceConnection: SessionServiceConnection
) : ViewModel()
