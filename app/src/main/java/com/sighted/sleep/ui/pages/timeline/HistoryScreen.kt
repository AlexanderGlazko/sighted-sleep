package com.sighted.sleep.ui.pages.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.dayText
import com.sighted.sleep.ui.entities.session.SessionListRow
import com.sighted.sleep.ui.features.tagfilter.TagFilterRow

/** All sessions as a list grouped by day (spec F5.8). Shown inside the Timeline tab. */
@Composable
fun HistoryList(
    onEditSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loaded && state.days.isEmpty() && state.filterTagId == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sleep logged yet.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        // Filter by tag (spec F5.8): only tags that are on at least one sleep.
        if (state.usedTags.isNotEmpty()) {
            item(key = "filter") {
                TagFilterRow(tags = state.usedTags, selectedTagId = state.filterTagId, onToggle = viewModel::toggleFilter)
            }
        }
        if (state.days.isEmpty()) {
            item(key = "empty") {
                Text(
                    "No sleeps with this tag.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
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
                SessionListRow(
                    session = row.session,
                    type = row.type,
                    targets = state.targets,
                    onClick = { onEditSession(row.session.id) },
                )
                HorizontalDivider()
            }
        }
    }
}
