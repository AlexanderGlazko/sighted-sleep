package com.sighted.sleep.ui.pages.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.features.edittiming.DateTimeField
import com.sighted.sleep.ui.features.ratesleep.CheckInForm
import com.sighted.sleep.ui.features.sleeptype.SleepTypeSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionScreen(
    onDone: () -> Unit,
    viewModel: EditSessionViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state = viewModel.state
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add sleep" else "Edit sleep") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DateTimeField("Fell asleep", state.startDate, state.startTime, viewModel::setStartDate, viewModel::setStartTime)

            if (state.canBeActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Still sleeping", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = state.stillSleeping, onCheckedChange = viewModel::setStillSleeping)
                }
            }
            if (!state.stillSleeping) {
                DateTimeField("Woke up", state.endDate, state.endTime, viewModel::setEndDate, viewModel::setEndTime)
            }

            viewModel.previewDuration()?.let {
                Text("Duration: ${formatDuration(it)}", style = MaterialTheme.typography.titleMedium)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                SleepTypeSwitch(type = state.type, onChange = viewModel::setType)
                Text(
                    "Auto: the longest sleep of the day is the main sleep, the rest are naps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!state.stillSleeping) {
                HorizontalDivider()
                val tags by viewModel.tags.collectAsStateWithLifecycle()
                CheckInForm(checkIn = state.checkIn, tags = tags, onChange = viewModel::setCheckIn)
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this sleep?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
