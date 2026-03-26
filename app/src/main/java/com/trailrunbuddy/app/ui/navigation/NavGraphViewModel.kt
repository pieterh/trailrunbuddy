package com.trailrunbuddy.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.trailrunbuddy.app.platform.service.SessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavGraphViewModel @Inject constructor(
    val sessionController: SessionController
) : ViewModel()
