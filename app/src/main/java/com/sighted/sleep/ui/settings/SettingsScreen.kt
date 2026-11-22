package com.sighted.sleep.ui.settings

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
import com.sighted.sleep.ui.checkin.ColorDot
import com.sighted.sleep.ui.checkin.tagColor
import kotlinx.coroutines.launch
import java.time.LocalTime

private val TAG_PALETTE = (PRESET_TAGS.map { it.second } + listOf("#90A4AE", "#FF8A65")).distinct()

/** Which dialog is open: a time picker, or the tag editor (tag == null means "new tag"). */
private sealed interface SettingsDialog {
    data object Bedtime : SettingsDialog
    data object Wake : SettingsDialog
    data class EditTag(val tag: Tag?) : SettingsDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomBar: @Composable () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        bottomBar = bottomBar,
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
