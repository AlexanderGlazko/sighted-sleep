package com.sighted.sleep.ui.features.workdays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Mon–Sun chips for work days, with the free-day note (spec §3.4). Figma: Settings → Work days. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkDaysPicker(
    workDays: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in workDays,
                    onClick = { onToggle(day) },
                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                )
            }
        }
        Text(
            "Sleep ending on other days counts as a free day (used for social jetlag). " +
                "The \"Day off\" tag also marks a free day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
