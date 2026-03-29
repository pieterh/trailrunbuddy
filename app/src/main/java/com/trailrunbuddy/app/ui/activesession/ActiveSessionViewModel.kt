package com.trailrunbuddy.app.ui.activesession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.platform.service.SessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ALERT_DURATION_MS = 5_000L

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val sessionController: SessionController
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveSessionUiState())
    val uiState: StateFlow<ActiveSessionUiState> = _uiState.asStateFlow()

    private val _events = Channel<ActiveSessionUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val alertJobs = mutableMapOf<Long, Job>()

    init {
        viewModelScope.launch {
            sessionController.alertEvents.collect { timerId ->
                alertJobs[timerId]?.cancel()
                _uiState.update { it.copy(firingTimerIds = it.firingTimerIds + timerId) }
                alertJobs[timerId] = launch {
                    delay(ALERT_DURATION_MS)
                    _uiState.update { it.copy(firingTimerIds = it.firingTimerIds - timerId) }
                }
            }
        }

        combine(
            sessionController.sessionState,
            sessionController.countdownStates
        ) { sessionState, countdownStates ->
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    sessionState = sessionState,
                    countdownStates = countdownStates
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onPauseResume() {
        when (_uiState.value.sessionState) {
            SessionState.RUNNING -> sessionController.pauseSession()
            SessionState.PAUSED -> sessionController.resumeSession()
            else -> Unit
        }
    }

    fun onStopRequested() = _uiState.update { it.copy(showStopConfirmDialog = true) }

    fun onStopConfirmed() {
        _uiState.update { it.copy(showStopConfirmDialog = false) }
        sessionController.stopSession()
        viewModelScope.launch {
            _events.send(ActiveSessionUiEvent.NavigateToProfileList)
        }
    }

    fun onStopDismissed() = _uiState.update { it.copy(showStopConfirmDialog = false) }
}
