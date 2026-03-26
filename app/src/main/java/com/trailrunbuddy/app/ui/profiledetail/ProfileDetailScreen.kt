package com.trailrunbuddy.app.ui.profiledetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trailrunbuddy.app.core.util.TimeFormatter
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.ui.components.ErrorText
import com.trailrunbuddy.app.ui.components.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profileId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileDetailUiEvent.SavedSuccessfully -> onNavigateBack()
                is ProfileDetailUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profileId == -1L) "New Profile" else "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::onSaveProfile,
                        enabled = !uiState.isSaving
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile header card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            name = uiState.name.ifBlank { "?" },
                            colorHex = com.trailrunbuddy.app.core.util.ColorGenerator.fromName(uiState.name)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = viewModel::onNameChange,
                                label = { Text("Profile name") },
                                isError = uiState.nameError != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            uiState.nameError?.let { ErrorText(it) }
                        }
                    }
                }
            }

            // Timers section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TIMERS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = viewModel::onShowAddTimer) {
                        Icon(Icons.Default.Add, contentDescription = "Add Timer")
                    }
                }
                HorizontalDivider()
                uiState.timersError?.let { ErrorText(it, modifier = Modifier.padding(top = 4.dp)) }
            }

            if (uiState.timers.isEmpty()) {
                item {
                    Text(
                        "No timers yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.timers, key = { it.id.toString() + it.name }) { timer ->
                    TimerListItem(
                        timer = timer,
                        onEdit = { viewModel.onEditTimer(timer) },
                        onDelete = { viewModel.onDeleteTimer(timer) }
                    )
                }
            }

            item { Spacer(Modifier.padding(bottom = 16.dp)) }
        }
    }

    // Add/Edit timer bottom sheet
    if (uiState.showAddTimerDialog) {
        AddTimerSheet(
            editingTimer = uiState.editingTimer,
            onDismiss = viewModel::onDismissTimerDialog,
            onSave = viewModel::onSaveTimer
        )
    }
}

@Composable
private fun TimerListItem(
    timer: Timer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(timer.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    TimeFormatter.formatHhMmSs(timer.durationSeconds.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilterChip(
                selected = timer.timerType == TimerType.REPEATING,
                onClick = {},
                label = {
                    Text(if (timer.timerType == TimerType.REPEATING) "Repeat" else "Once")
                }
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTimerSheet(
    editingTimer: Timer?,
    onDismiss: () -> Unit,
    onSave: (name: String, durationSeconds: Int, timerType: TimerType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(editingTimer?.name ?: "") }
    var hours by remember { mutableIntStateOf((editingTimer?.durationSeconds ?: 0) / 3600) }
    var minutes by remember { mutableIntStateOf(((editingTimer?.durationSeconds ?: 0) % 3600) / 60) }
    var timerType by remember { mutableStateOf(editingTimer?.timerType ?: TimerType.REPEATING) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var durationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (editingTimer != null) "Edit Timer" else "Add Timer",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("Timer name") },
                isError = nameError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            nameError?.let { ErrorText(it) }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (hours == 0) "" else hours.toString(),
                    onValueChange = { hours = it.toIntOrNull() ?: 0; durationError = null },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = if (minutes == 0) "" else minutes.toString(),
                    onValueChange = { minutes = (it.toIntOrNull() ?: 0).coerceIn(0, 59); durationError = null },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            durationError?.let { ErrorText(it) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = timerType == TimerType.REPEATING,
                    onClick = { timerType = TimerType.REPEATING },
                    label = { Text("Repeating") }
                )
                FilterChip(
                    selected = timerType == TimerType.ONCE,
                    onClick = { timerType = TimerType.ONCE },
                    label = { Text("Once") }
                )
            }

            Button(
                onClick = {
                    val totalSeconds = hours * 3600 + minutes * 60
                    if (name.isBlank()) {
                        nameError = "Timer name cannot be empty"
                        return@Button
                    }
                    if (totalSeconds <= 0) {
                        durationError = "Duration must be greater than 0"
                        return@Button
                    }
                    onSave(name, totalSeconds, timerType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Save Timer")
            }
        }
    }
}
