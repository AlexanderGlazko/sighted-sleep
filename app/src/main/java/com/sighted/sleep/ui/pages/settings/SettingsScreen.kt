package com.sighted.sleep.ui.pages.settings

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.reminders.Notifier
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.features.backup.BackupSection
import com.sighted.sleep.ui.features.backup.ReplaceAllDataDialog
import com.sighted.sleep.ui.features.managetags.TagEditorDialog
import com.sighted.sleep.ui.features.managetags.tagListItems
import com.sighted.sleep.ui.features.reminders.RemindersSection
import com.sighted.sleep.ui.features.settarget.TargetSchedule
import com.sighted.sleep.ui.features.workdays.WorkDaysPicker
import com.sighted.sleep.ui.kit.molecules.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** The tag being edited in the tag dialog; tag == null means "new tag". */
private data class TagEdit(val tag: Tag?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomBar: @Composable () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tagEdit by remember { mutableStateOf<TagEdit?>(null) }
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
            item { TargetSchedule(state.targets, onBedtime = viewModel::setBedtime, onWake = viewModel::setWake) }
            item { HorizontalDivider(Modifier.padding(top = 12.dp)) }

            item { SectionHeader("Work days") }
            item { WorkDaysPicker(state.workDays, onToggle = viewModel::toggleWorkDay) }
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
            tagListItems(state.tags, onEdit = { tagEdit = TagEdit(it) }, onAdd = { tagEdit = TagEdit(null) })
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

    tagEdit?.let { edit ->
        val tag = edit.tag
        TagEditorDialog(
            tag = tag,
            onSave = { name, color, archived -> viewModel.saveTag(tag, name, color, archived) },
            onDelete = { if (tag != null) viewModel.deleteTag(tag) else null },
            onDone = { tagEdit = null },
        )
    }

    importPreview?.let { p ->
        ReplaceAllDataDialog(
            exportedAt = p.exportedAt,
            sleepCount = p.file.sessions.size,
            tagCount = p.file.tags.size,
            onConfirm = {
                importPreview = null
                scope.launch { say(runCatching { viewModel.import(p) }.getOrElse { "Import failed. Nothing was changed." }) }
            },
            onDismiss = { importPreview = null },
        )
    }
}
