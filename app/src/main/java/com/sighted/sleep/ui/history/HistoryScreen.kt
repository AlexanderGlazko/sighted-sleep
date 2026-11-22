package com.sighted.sleep.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.clickable
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.dayText
import com.sighted.sleep.ui.label
import com.sighted.sleep.ui.rangeText
import com.sighted.sleep.ui.deviationText
import com.sighted.sleep.domain.model.SessionType
import androidx.compose.foundation.layout.Column
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEditSession: (Long) -> Unit,
    onAddSession: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSession) {
                Icon(Icons.Filled.Add, contentDescription = "Add sleep manually")
            }
        },
    ) { padding ->
        if (state.loaded && state.days.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No sleep logged yet.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            state.days.forEach { day ->
                item(key = "day-${day.date}") {
                    Text(
                        day.date?.dayText() ?: "Now",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                items(day.rows, key = { it.session.id }) { row ->
                    val s = row.session
                    ListItem(
                        headlineContent = { Text(s.rangeText()) },
                        supportingContent = {
                            Column {
                                val rating = when {
                                    s.isActive -> null
                                    s.quality != null -> "${s.quality}/10"
                                    else -> "not rated"
                                }
                                Text(listOfNotNull(row.type?.label() ?: "Sleeping now", rating).joinToString(" · "))
                                if (row.type == SessionType.MAIN) {
                                    s.deviationText(state.targets)?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Text(
                                formatDuration(s.duration(s.end ?: Instant.now())),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        modifier = Modifier.clickable { onEditSession(s.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
