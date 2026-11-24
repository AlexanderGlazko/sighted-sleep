package com.sighted.sleep.ui.widgets.lastsleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.dayText
import com.sighted.sleep.ui.deviationText
import com.sighted.sleep.ui.entities.tag.TagLabel
import com.sighted.sleep.ui.rangeText

/**
 * Last main sleep with its naps (spec F5.1). Figma: Widgets / Last sleep / Card.
 * [tags] are the tags on [main]; an unrated sleep shows "Rate this sleep" instead of the quality.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LastSleepCard(
    main: SleepSession,
    naps: List<SleepSession>,
    targets: Targets,
    tags: List<Tag>,
    onOpen: () -> Unit,
    onRate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Last sleep · ${main.endDate!!.dayText()}", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(main.rangeText(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(formatDuration(main.duration(main.end!!)), style = MaterialTheme.typography.titleLarge)
            }
            main.deviationText(targets)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    tags.forEach { TagLabel(it) }
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
