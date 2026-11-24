package com.sighted.sleep.ui.pages.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.data.db.PRESET_TAGS
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.kit.atoms.ColorDot
import com.sighted.sleep.ui.entities.tag.tagColor
import kotlinx.coroutines.launch
import android.content.pm.ApplicationInfo
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sighted.sleep.reminders.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.LocalContext
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.time.LocalTime

private val TAG_PALETTE = (PRESET_TAGS.map { it.second } + listOf("#90A4AE", "#FF8A65")).distinct()

/** Which dialog is open: a time picker, or the tag editor (tag == null means "new tag"). */
private sealed interface SettingsDialog {
    data object Bedtime : SettingsDialog
    data object Wake : SettingsDialog
    data class EditTag(val tag: Tag?) : SettingsDialog
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    bottomBar: @Composable () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDebugBuild = remember { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    fun say(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }

    // Refreshed every time the screen comes back (e.g. after the user changes system settings).
    var canNotify by remember { mutableStateOf(Notifier.canNotify(context)) }
    var batteryOptimized by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canNotify = Notifier.canNotify(context)
        batteryOptimized = !context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        if (!granted) say("Notifications are blocked, so reminders can't appear.")
    }

    // Backup: the system file picker chooses where files go, so no storage permission is needed.
    fun writeTo(uri: Uri?, produce: suspend () -> String, done: String) {
        uri ?: return
        scope.launch {
            val ok = runCatching {
                val text = produce()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }
            }.isSuccess
            say(if (ok) done else "Couldn't save the file.")
        }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        writeTo(it, viewModel::exportJson, "Backup saved.")
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        writeTo(it, viewModel::exportCsv, "CSV saved.")
    }
    var importPreview by remember { mutableStateOf<ImportPreview?>(null) }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
            }.getOrNull()
            if (text == null) { say("Couldn't read the file."); return@launch }
            val (preview, error) = viewModel.previewImport(text)
            if (preview != null) importPreview = preview else say(error ?: "Invalid file.")
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionHeader("Target schedule") }
            item {
                ListItem(
                    headlineContent = { Text("Bedtime") },
                    trailingContent = { Text(state.targets.bedtime.toString(), style = MaterialTheme.typography.titleMedium) },
                    modifier = Modifier.clickable { dialog = SettingsDialog.Bedtime },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Wake time") },
                    trailingContent = { Text(state.targets.wake.toString(), style = MaterialTheme.typography.titleMedium) },
                    modifier = Modifier.clickable { dialog = SettingsDialog.Wake },
                )
            }
            item {
                Text(
                    "Target sleep: ${formatDuration(state.targets.duration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item { HorizontalDivider(Modifier.padding(top = 12.dp)) }

            item { SectionHeader("Work days") }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in state.workDays,
                            onClick = { viewModel.toggleWorkDay(day) },
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
            item { HorizontalDivider(Modifier.padding(top = 12.dp)) }

            item { SectionHeader("Reminders") }
            item {
                RemindersSection(
                    r = state.reminders,
                    canNotify = canNotify,
                    batteryOptimized = batteryOptimized,
                    onChange = { change ->
                        viewModel.updateReminders(change)
                        // Ask for notification permission the first time a reminder is switched on.
                        if (!Notifier.canNotify(context)) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onOpenNotificationSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    onOpenBatterySettings = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                )
            }
            item { HorizontalDivider(Modifier.padding(top = 12.dp)) }

            item { SectionHeader("Tags") }
            items(state.tags, key = { it.id }) { tag ->
                ListItem(
                    leadingContent = { ColorDot(tagColor(tag.colorHex), size = 14) },
                    headlineContent = { Text(tag.name) },
                    supportingContent = if (tag.isArchived) ({ Text("Archived") }) else null,
                    modifier = Modifier.clickable { dialog = SettingsDialog.EditTag(tag) },
                )
            }
            item {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                    headlineContent = { Text("Add tag") },
                    modifier = Modifier.clickable { dialog = SettingsDialog.EditTag(null) },
                )
            }
            item { HorizontalDivider(Modifier.padding(top = 12.dp)) }

            item { SectionHeader("Backup") }
            item {
                BackupSection(
                    onExportJson = { exportJson.launch("sighted-sleep-backup-${LocalDate.now()}.json") },
                    onExportCsv = { exportCsv.launch("sighted-sleep-${LocalDate.now()}.csv") },
                    onImport = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                )
            }

            if (isDebugBuild) {
                item { HorizontalDivider(Modifier.padding(top = 12.dp)) }
                item { SectionHeader("Developer (debug build only)") }
                item {
                    ListItem(
                        headlineContent = { Text("Add 90 days of sample data") },
                        supportingContent = { Text("Only when the log is empty") },
                        modifier = Modifier.clickable { scope.launch { snackbar.showSnackbar(viewModel.addSampleData()) } },
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Remove sample data") },
                        supportingContent = { Text("Real sleeps are never touched") },
                        modifier = Modifier.clickable { scope.launch { snackbar.showSnackbar(viewModel.removeSampleData()) } },
                    )
                }
            }
        }
    }

    when (val d = dialog) {
        SettingsDialog.Bedtime -> TimeDialog(state.targets.bedtime, onDismiss = { dialog = null }) {
            viewModel.setBedtime(it); dialog = null
        }
        SettingsDialog.Wake -> TimeDialog(state.targets.wake, onDismiss = { dialog = null }) {
            viewModel.setWake(it); dialog = null
        }
        is SettingsDialog.EditTag -> TagDialog(d.tag, viewModel, onDone = { dialog = null })
        null -> Unit
    }

    importPreview?.let { p ->
        AlertDialog(
            onDismissRequest = { importPreview = null },
            title = { Text("Replace all data?") },
            text = {
                Text(
                    "Backup from ${p.exportedAt}: ${p.file.sessions.size} sleeps, ${p.file.tags.size} tags and its settings.\n\n" +
                        "Everything currently in the app will be replaced. This can't be undone — " +
                        "export a backup of the current data first if you might need it.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importPreview = null
                    scope.launch { say(runCatching { viewModel.import(p) }.getOrElse { "Import failed. Nothing was changed." }) }
                }) { Text("Replace", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { importPreview = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val picker = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(LocalTime.of(picker.hour, picker.minute)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = picker) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagDialog(tag: Tag?, viewModel: SettingsViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf(tag?.name.orEmpty()) }
    var color by remember { mutableStateOf(tag?.colorHex ?: TAG_PALETTE.first()) }
    var archived by remember { mutableStateOf(tag?.isArchived ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (tag == null) "New tag" else "Edit tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TAG_PALETTE.forEach { hex ->
                        val selected = hex == color
                        Row(
                            Modifier
                                .size(32.dp)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                )
                                .clickable { color = hex },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) { ColorDot(tagColor(hex), size = if (selected) 22 else 28) }
                    }
                }
                if (tag != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Archived")
                            Text(
                                "Hidden from check-in; past sleeps keep it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = archived, onCheckedChange = { archived = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    error = viewModel.saveTag(tag, name, color, archived)
                    if (error == null) onDone()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (tag != null && !tag.isPreset) {
                    TextButton(onClick = {
                        scope.launch {
                            error = viewModel.deleteTag(tag)
                            if (error == null) onDone()
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDone) { Text("Cancel") }
            }
        },
    )
}
