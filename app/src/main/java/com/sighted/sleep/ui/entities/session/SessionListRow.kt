package com.sighted.sleep.ui.entities.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.deviationText
import com.sighted.sleep.ui.label
import com.sighted.sleep.ui.rangeText
import java.time.Instant

/**
 * One sleep in a list: times, type and rating, the deviation from target for a main sleep,
 * and the duration (spec F5.8). Figma: Entities / Sleep session / List row, Type = Main / Nap.
 * [type] is null for a sleep that is still running.
 */
@Composable
fun SessionListRow(
    session: SleepSession,
    type: SessionType?,
    targets: Targets,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(session.rangeText()) },
        supportingContent = {
            Column {
                val rating = when {
                    session.isActive -> null
                    session.quality != null -> "${session.quality}/10"
                    else -> "not rated"
                }
                Text(listOfNotNull(type?.label() ?: "Sleeping now", rating).joinToString(" · "))
                if (type == SessionType.MAIN) {
                    session.deviationText(targets)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        trailingContent = {
            Text(
                formatDuration(session.duration(session.end ?: Instant.now())),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        modifier = modifier.clickable(onClick = onClick),
    )
}
