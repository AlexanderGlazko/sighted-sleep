package com.sighted.sleep.ui.pages.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.model.ReminderSettings

/** Reminders (spec F6): three independent switches, each with its timing option. */
@Composable
fun RemindersSection(
    r: ReminderSettings,
    canNotify: Boolean,
    batteryOptimized: Boolean,
    onChange: ((ReminderSettings) -> ReminderSettings) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    Column {
        ReminderRow(
            title = "Bedtime reminder",
            subtitle = "Before your target bedtime",
            checked = r.bedtimeEnabled,
            onChecked = { on -> onChange { it.copy(bedtimeEnabled = on) } },
        )
        if (r.bedtimeEnabled) {
            Choices(listOf(15 to "15 min", 30 to "30 min", 60 to "1 hour"), r.bedtimeOffsetMin) { v ->
                onChange { it.copy(bedtimeOffsetMin = v) }
            }
        }
        ReminderRow(
            title = "Morning check-in",
            subtitle = "If last night isn't rated yet",
            checked = r.morningEnabled,
            onChecked = { on -> onChange { it.copy(morningEnabled = on) } },
        )
        if (r.morningEnabled) {
            Choices(listOf(30 to "30 min", 60 to "1 hour", 120 to "2 hours"), r.morningDelayMin, prefix = "after waking") { v ->
                onChange { it.copy(morningDelayMin = v) }
            }
        }
        ReminderRow(
            title = "\"Still sleeping?\" alert",
            subtitle = "If a sleep runs over 14 hours",
            checked = r.stillSleepingEnabled,
            onChecked = { on -> onChange { it.copy(stillSleepingEnabled = on) } },
        )

        if (r.anyEnabled && !canNotify) {
            Warning("Notifications are blocked for Sighted Sleep, so reminders can't appear.", "Allow", onOpenNotificationSettings)
        }
        if (r.anyEnabled && batteryOptimized) {
            Warning("Battery optimisation can delay reminders on Nothing OS.", "Settings", onOpenBatterySettings)
        }
    }
}

@Composable
private fun ReminderRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChecked) },
    )
}

@Composable
private fun Choices(options: List<Pair<Int, String>>, selected: Int, prefix: String = "before", onPick: (Int) -> Unit) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (v, label) ->
            FilterChip(selected = v == selected, onClick = { onPick(v) }, label = { Text("$label $prefix") })
        }
    }
}

@Composable
private fun Warning(text: String, action: String, onAction: () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAction) { Text(action) }
    }
}

/** Backup (spec F7.2, F7.3). */
@Composable
fun BackupSection(onExportJson: () -> Unit, onExportCsv: () -> Unit, onImport: () -> Unit) {
    Column {
        ListItem(
            headlineContent = { Text("Export backup (JSON)") },
            supportingContent = { Text("All sleeps, tags and settings. Restorable with Import.") },
            modifier = Modifier.clickableRow(onExportJson),
        )
        ListItem(
            headlineContent = { Text("Export sleeps as CSV") },
            supportingContent = { Text("For spreadsheets. Can't be imported back.") },
            modifier = Modifier.clickableRow(onExportCsv),
        )
        ListItem(
            headlineContent = { Text("Import backup") },
            supportingContent = { Text("Replaces all current data with a JSON backup") },
            modifier = Modifier.clickableRow(onImport),
        )
        Text(
            "Export before uninstalling or switching phones: there's no cloud copy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit) = this.clickable(onClick = onClick)
