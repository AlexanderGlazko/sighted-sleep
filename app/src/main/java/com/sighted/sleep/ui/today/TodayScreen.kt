package com.sighted.sleep.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.sighted.sleep.domain.formatElapsed
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.reminders.AppRequest
import com.sighted.sleep.ui.checkin.CheckInForm
import com.sighted.sleep.ui.checkin.ColorDot
import com.sighted.sleep.ui.checkin.tagColor
import com.sighted.sleep.ui.dayText
import com.sighted.sleep.ui.deviationText
import com.sighted.sleep.ui.rangeText
import com.sighted.sleep.ui.startTimeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    request: AppRequest?,
    onEditSession: (Long) -> Unit,
    onAddSession: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(request?.token) { request?.let(viewModel::onRequest) }
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
                Text(formatElapsed(state.activeDuration!!), style = MaterialTheme.typography.displayMedium)
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

            state.latest.main?.let { main ->
                LastSleepCard(
                    main = main,
                    naps = state.latest.napsAfter,
                    state = state,
                    onOpen = { onEditSession(main.id) },
                    onRate = { viewModel.openCheckIn(main.id) },
                )
            }

            if (state.latest.main != null) {
                Text(
                    "Sleep debt (7 days): " +
                        if (state.sleepDebt.isZero) "none" else formatDuration(state.sleepDebt),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                "Target: ${state.targets.bedtime} → ${state.targets.wake} (${formatDuration(state.targets.duration)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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

    viewModel.checkInDraft?.let { draft ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = viewModel::skipCheckIn, sheetState = sheetState) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("How did you sleep?", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${draft.session.rangeText()} · ${formatDuration(draft.session.duration(draft.session.end ?: state.now))}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CheckInForm(checkIn = draft.checkIn, tags = state.tags, onChange = viewModel::updateCheckIn)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = viewModel::skipCheckIn, modifier = Modifier.weight(1f)) { Text("Skip") }
                    Button(onClick = { viewModel.saveCheckIn() }, modifier = Modifier.weight(1f)) { Text("Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LastSleepCard(
    main: SleepSession,
    naps: List<SleepSession>,
    state: TodayUiState,
    onOpen: () -> Unit,
    onRate: () -> Unit,
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Last sleep · ${main.endDate!!.dayText()}", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(main.rangeText(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(formatDuration(main.duration(main.end!!)), style = MaterialTheme.typography.titleLarge)
            }
            main.deviationText(state.targets)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val tagIds = state.sessionTags[main.id].orEmpty()
            val tags = state.tags.filter { it.id in tagIds }
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    tags.forEach { tag ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(tagColor(tag.colorHex), size = 8)
                            Text(" ${tag.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (main.quality != null) {
                Text("Quality ${main.quality}/10", style = MaterialTheme.typography.bodyLarge)
            } else {
                FilledTonalButton(onClick = onRate) { Text("Rate this sleep") }
            }

            if (naps.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                naps.forEach { nap ->
                    Text(
                        "Nap · ${nap.rangeText()} · ${formatDuration(nap.duration(nap.end!!))}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
