package com.sighted.sleep.ui.pages.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.dayText
import com.sighted.sleep.ui.deviationText
import com.sighted.sleep.ui.label
import com.sighted.sleep.ui.rangeText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import com.sighted.sleep.ui.features.ratesleep.ColorDot
import com.sighted.sleep.ui.features.ratesleep.tagColor
import java.time.Instant

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
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.usedTags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = state.filterTagId == tag.id,
                            onClick = { viewModel.toggleFilter(tag.id) },
                            label = { Text(tag.name) },
                            leadingIcon = { ColorDot(tagColor(tag.colorHex)) },
                        )
                    }
                }
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
