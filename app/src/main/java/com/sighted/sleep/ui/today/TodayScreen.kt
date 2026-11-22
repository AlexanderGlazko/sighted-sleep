package com.sighted.sleep.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.dayText
import com.sighted.sleep.ui.label
import com.sighted.sleep.ui.rangeText
import com.sighted.sleep.ui.startTimeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onEditSession: (Long) -> Unit,
    onAddSession: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sighted Sleep") }) },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.loaded) return@Column
            val active = state.active

            if (active != null) {
                Text("Sleeping since ${active.startTimeText()}", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatDuration(state.activeDuration!!),
                    style = MaterialTheme.typography.displayMedium,
                )
            } else {
                Text("Awake", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (state.stillSleepingWarning && active != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Still sleeping?", style = MaterialTheme.typography.titleMedium)
                        Text("This sleep has been running for over 14 hours. Did you forget to tap Wake up?")
                        TextButton(onClick = { onEditSession(active.id) }) { Text("Edit end time") }
                    }
                }
            }

            Button(
                onClick = { if (active != null) viewModel.onWake() else viewModel.onSleep() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                colors = if (active != null) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text(
                    if (active != null) "Wake up" else "Go to sleep",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            state.lastSleep?.let { last ->
                Card(onClick = { onEditSession(last.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Last sleep · ${last.endDate!!.dayText()}" +
                                (state.lastSleepType?.let { " · ${it.label()}" } ?: ""),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(last.rangeText(), style = MaterialTheme.typography.titleLarge)
                        Text(formatDuration(last.duration(last.end!!)), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            OutlinedButton(onClick = onAddSession) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add sleep manually")
            }
        }
    }

    viewModel.discardCandidate?.let { short ->
        AlertDialog(
            onDismissRequest = viewModel::onKeepShortSession,
            title = { Text("Discard this session?") },
            text = { Text("It lasted only ${formatDuration(short.duration(short.end!!))}. Was it an accidental tap?") },
            confirmButton = { TextButton(onClick = { viewModel.onDiscardConfirmed() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = viewModel::onKeepShortSession) { Text("Keep") } },
        )
    }
}
