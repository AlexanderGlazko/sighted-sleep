package com.sighted.sleep.ui.features.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Backup (spec F7.2, F7.3): export JSON, export CSV, import. Figma: Settings → Backup. */
@Composable
fun BackupSection(onExportJson: () -> Unit, onExportCsv: () -> Unit, onImport: () -> Unit) {
    Column {
        ListItem(
            headlineContent = { Text("Export backup (JSON)") },
            supportingContent = { Text("All sleeps, tags and settings. Restorable with Import.") },
            modifier = Modifier.clickable(onClick = onExportJson),
        )
        ListItem(
            headlineContent = { Text("Export sleeps as CSV") },
            supportingContent = { Text("For spreadsheets. Can't be imported back.") },
            modifier = Modifier.clickable(onClick = onExportCsv),
        )
        ListItem(
            headlineContent = { Text("Import backup") },
            supportingContent = { Text("Replaces all current data with a JSON backup") },
            modifier = Modifier.clickable(onClick = onImport),
        )
        Text(
            "Export before uninstalling or switching phones: there's no cloud copy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

/** Confirmation before an import replaces everything (spec F7.3). Vault note: "Replace all data". */
@Composable
fun ReplaceAllDataDialog(
    exportedAt: String,
    sleepCount: Int,
    tagCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace all data?") },
        text = {
            Text(
                "Backup from $exportedAt: $sleepCount sleeps, $tagCount tags and its settings.\n\n" +
                    "Everything currently in the app will be replaced. This can't be undone — " +
                    "export a backup of the current data first if you might need it.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Replace", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
