package com.sighted.sleep.ui.features.managetags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.data.db.PRESET_TAGS
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.ui.entities.tag.tagColor
import com.sighted.sleep.ui.kit.atoms.ColorDot
import com.sighted.sleep.ui.kit.atoms.ColourSwatch
import kotlinx.coroutines.launch

/** Colours a tag can have: the preset tag colours plus two extras. */
val TAG_PALETTE: List<String> = (PRESET_TAGS.map { it.second } + listOf("#90A4AE", "#FF8A65")).distinct()

/**
 * Tag manager list (spec F3.2): one row per tag (archived ones marked) and an "Add tag" row.
 * Added as LazyColumn items so each tag keeps its own key. Figma: Settings → Tags.
 */
fun LazyListScope.tagListItems(tags: List<Tag>, onEdit: (Tag) -> Unit, onAdd: () -> Unit) {
    items(tags, key = { it.id }) { tag ->
        ListItem(
            leadingContent = { ColorDot(tagColor(tag.colorHex), size = 14) },
            headlineContent = { Text(tag.name) },
            supportingContent = if (tag.isArchived) ({ Text("Archived") }) else null,
            modifier = Modifier.clickable { onEdit(tag) },
        )
    }
    item {
        ListItem(
            leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
            headlineContent = { Text("Add tag") },
            modifier = Modifier.clickable(onClick = onAdd),
        )
    }
}

/**
 * New tag ([tag] == null) or Edit tag (spec F3.2). [onSave] and [onDelete] return an error message, or null when done.
 * Delete shows only for a custom tag. Figma: New tag (dialog), Edit tag (dialog).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditorDialog(
    tag: Tag?,
    onSave: suspend (name: String, colorHex: String, archived: Boolean) -> String?,
    onDelete: suspend () -> String?,
    onDone: () -> Unit,
) {
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
                        ColourSwatch(tagColor(hex), selected = hex == color, onClick = { color = hex })
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
                    error = onSave(name, color, archived)
                    if (error == null) onDone()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (tag != null && !tag.isPreset) {
                    TextButton(onClick = {
                        scope.launch {
                            error = onDelete()
                            if (error == null) onDone()
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDone) { Text("Cancel") }
            }
        },
    )
}
