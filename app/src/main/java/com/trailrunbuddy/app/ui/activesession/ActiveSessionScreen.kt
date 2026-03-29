package com.trailrunbuddy.app.ui.activesession

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.platform.timer.TimerCountdownState
import com.trailrunbuddy.app.ui.components.ConfirmationDialog
import com.trailrunbuddy.app.ui.components.CountdownDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    profileId: Long,
    onNavigateToProfileList: () -> Unit,
    viewModel: ActiveSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ActiveSessionUiEvent.NavigateToProfileList -> onNavigateToProfileList()
            }
        }
    }

    BackHandler { viewModel.onStopRequested() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Active") },
                actions = {
                    IconButton(onClick = viewModel::onStopRequested) {
                        Icon(Icons.Default.Close, contentDescription = "Stop Session")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // States are sorted by profileItemSortOrder from the engine.
            // Walk the list and collapse consecutive group states into one card.
            val states = uiState.countdownStates
            val groupedStates = states.filter { it.isInGroup }
            var groupEmitted = false

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                states.forEach { state ->
                    if (state.isInGroup) {
                        if (!groupEmitted) {
                            groupEmitted = true
                            item(key = "group_container") { GroupCountdownCard(groupedStates) }
                        }
                    } else {
                        item(key = state.timer.id) { TimerCountdownCard(state) }
                    }
                }
            }

            SessionControls(
                sessionState = uiState.sessionState,
                onPauseResume = viewModel::onPauseResume,
                onStop = viewModel::onStopRequested,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (uiState.showStopConfirmDialog) {
        ConfirmationDialog(
            title = "Stop session?",
            message = "Your current session progress will be lost.",
            confirmLabel = "Stop",
            onConfirm = viewModel::onStopConfirmed,
            onDismiss = viewModel::onStopDismissed
        )
    }
}

@Composable
private fun GroupCountdownCard(groupStates: List<TimerCountdownState>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "GROUP",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            groupStates.forEach { state ->
                if (state.isActiveInGroup) {
                    TimerCountdownCard(state)
                } else {
                    WaitingTimerCard(state)
                }
            }
        }
    }
}

@Composable
private fun WaitingTimerCard(state: TimerCountdownState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.timer.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "waiting",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun TimerCountdownCard(state: TimerCountdownState) {
    val durationMs = state.timer.durationSeconds * 1000L
    val progress = if (durationMs > 0 && !state.isFinished) {
        (state.remainingMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        if (state.isFinished) 0f else 1f
    }

    val containerColor = when {
        state.isFinished -> MaterialTheme.colorScheme.surfaceVariant
        state.isPreWarning -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.timer.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.timer.timerType.name == "REPEATING" && state.cycleCount > 0) {
                    Text(
                        text = "×${state.cycleCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (state.isFinished) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CountdownDisplay(
                    remainingMs = state.remainingMs,
                    isPreWarning = state.isPreWarning
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SessionControls(
    sessionState: SessionState?,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPauseResume,
            modifier = Modifier.weight(1f)
        ) {
            Text(if (sessionState == SessionState.PAUSED) "Resume" else "Pause")
        }
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Stop")
        }
    }
}
