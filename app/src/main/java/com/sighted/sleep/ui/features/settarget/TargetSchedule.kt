package com.sighted.sleep.ui.features.settarget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.kit.organisms.TimeDialog
import java.time.LocalTime

/**
 * Target bedtime and wake time, each opening a time picker, plus the derived target sleep (spec F4.1).
 * Figma: Settings → Target schedule, Bedtime picker (dialog).
 */
@Composable
fun TargetSchedule(
    targets: Targets,
    onBedtime: (LocalTime) -> Unit,
    onWake: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<String?>(null) }

    Column(modifier) {
        ListItem(
            headlineContent = { Text("Bedtime") },
            trailingContent = { Text(targets.bedtime.toString(), style = MaterialTheme.typography.titleMedium) },
            modifier = Modifier.clickable { editing = "bed" },
        )
        ListItem(
            headlineContent = { Text("Wake time") },
            trailingContent = { Text(targets.wake.toString(), style = MaterialTheme.typography.titleMedium) },
            modifier = Modifier.clickable { editing = "wake" },
        )
        Text(
            "Target sleep: ${formatDuration(targets.duration)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    when (editing) {
        "bed" -> TimeDialog(targets.bedtime, onDismiss = { editing = null }) { onBedtime(it); editing = null }
        "wake" -> TimeDialog(targets.wake, onDismiss = { editing = null }) { onWake(it); editing = null }
    }
}
