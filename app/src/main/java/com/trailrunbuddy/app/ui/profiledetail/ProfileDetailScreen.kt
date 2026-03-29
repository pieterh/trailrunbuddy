package com.trailrunbuddy.app.ui.profiledetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.trailrunbuddy.app.domain.model.ProfileItem
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerType
import com.trailrunbuddy.app.ui.components.ConfirmationDialog
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

            // Items section header
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
                    // "+" button with dropdown menu
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add timer") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.onShowAddStandaloneTimer()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Add group",
                                        color = if (uiState.hasGroup)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.onAddGroup()
                                },
                                enabled = !uiState.hasGroup
                            )
                        }
                    }
                }
                HorizontalDivider()
                uiState.timersError?.let { ErrorText(it, modifier = Modifier.padding(top = 4.dp)) }
            }

            if (uiState.items.isEmpty()) {
                item {
                    Text(
                        "No timers yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.items, key = { item ->
                    when (item) {
                        is ProfileItem.StandaloneTimer -> "timer_${item.timer.id}_${item.timer.name}"
                        is ProfileItem.Group -> "group"
                    }
                }) { item ->
                    when (item) {
                        is ProfileItem.StandaloneTimer -> TimerListItem(
                            timer = item.timer,
                            onEdit = { viewModel.onEditTimer(item.timer) },
                            onDelete = { viewModel.onDeleteTimer(item.timer) }
                        )
                        is ProfileItem.Group -> GroupCard(
                            group = item,
                            onAddTimer = viewModel::onShowAddTimerToGroup,
                            onEditTimer = { viewModel.onEditTimer(it) },
                            onDeleteTimer = { viewModel.onDeleteTimer(it) },
                            onDeleteGroup = viewModel::onRequestDeleteGroup,
                            onTimerTypeChange = viewModel::onGroupTimerTypeChange
                        )
                    }
                }
            }

            item { Spacer(Modifier.padding(bottom = 16.dp)) }
        }
    }

    if (uiState.showAddTimerDialog) {
        AddTimerSheet(
            editingTimer = uiState.editingTimer,
            showTimerTypeSelection = !uiState.addingTimerToGroup && !uiState.editingTimerIsGrouped,
            onDismiss = viewModel::onDismissTimerDialog,
            onSave = viewModel::onSaveTimer
        )
    }

    if (uiState.showDeleteGroupDialog) {
        ConfirmationDialog(
            title = "Delete group?",
            message = "All timers inside the group will also be deleted.",
            confirmLabel = "Delete",
            onConfirm = viewModel::onConfirmDeleteGroup,
            onDismiss = viewModel::onDismissDeleteGroup
        )
    }
}

@Composable
private fun GroupCard(
    group: ProfileItem.Group,
    onAddTimer: () -> Unit,
    onEditTimer: (Timer) -> Unit,
    onDeleteTimer: (Timer) -> Unit,
    onDeleteGroup: () -> Unit,
    onTimerTypeChange: (TimerType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Group header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GROUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = group.group.timerType == TimerType.REPEATING,
                        onClick = { onTimerTypeChange(TimerType.REPEATING) },
                        label = { Text("Repeat") }
                    )
                    FilterChip(
                        selected = group.group.timerType == TimerType.ONCE,
                        onClick = { onTimerTypeChange(TimerType.ONCE) },
                        label = { Text("Once") }
                    )
                    IconButton(onClick = onDeleteGroup) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete group",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            HorizontalDivider()

            if (group.group.timers.isEmpty()) {
                Text(
                    "No timers in group yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                group.group.timers.forEach { timer ->
                    GroupedTimerItem(
                        timer = timer,
                        onEdit = { onEditTimer(timer) },
                        onDelete = { onDeleteTimer(timer) }
                    )
                }
            }

            TextButton(
                onClick = onAddTimer,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Add timer to group")
            }
        }
    }
}

@Composable
private fun GroupedTimerItem(
    timer: Timer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
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
    showTimerTypeSelection: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, durationSeconds: Int, timerType: TimerType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(editingTimer?.name ?: "") }
    val d = editingTimer?.durationSeconds ?: 0
    var hoursText by remember { mutableStateOf(if (d / 3600 > 0) (d / 3600).toString() else "") }
    var minutesText by remember { mutableStateOf(if ((d % 3600) / 60 > 0) ((d % 3600) / 60).toString() else "") }
    var secondsText by remember { mutableStateOf(if (d % 60 > 0) (d % 60).toString() else "") }
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter(Char::isDigit); durationError = null },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit); durationError = null },
                    label = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it.filter(Char::isDigit); durationError = null },
                    label = { Text("Sec") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            durationError?.let { ErrorText(it) }

            if (showTimerTypeSelection) {
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
            }

            Button(
                onClick = {
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minutesText.toIntOrNull() ?: 0
                    val s = secondsText.toIntOrNull() ?: 0
                    if (name.isBlank()) {
                        nameError = "Timer name cannot be empty"
                        return@Button
                    }
                    if (m > 59 || s > 59) {
                        durationError = "Minutes and seconds must be 0–59"
                        return@Button
                    }
                    val totalSeconds = h * 3600 + m * 60 + s
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
