package com.sighted.sleep.ui.features.ratesleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.model.CheckIn
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.ui.entities.tag.tagColor
import com.sighted.sleep.ui.kit.atoms.ColorDot

/** Quality 1–10, tags and comment (spec F2.1). Shared by the morning sheet and the edit screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInForm(
    checkIn: CheckIn,
    tags: List<Tag>,
    onChange: (CheckIn) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("How rested do you feel? (1–10)", style = MaterialTheme.typography.labelLarge)
        ScoreScale(value = checkIn.quality, onChange = { onChange(checkIn.copy(quality = it)) })

        Text("Tags", style = MaterialTheme.typography.labelLarge)
        // Archived tags stay visible only if this session already has them.
        val visible = tags.filter { !it.isArchived || it.id in checkIn.tagIds }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            visible.forEach { tag ->
                val selected = tag.id in checkIn.tagIds
                FilterChip(
                    selected = selected,
                    onClick = {
                        val ids = if (selected) checkIn.tagIds - tag.id else checkIn.tagIds + tag.id
                        onChange(checkIn.copy(tagIds = ids))
                    },
                    label = { Text(tag.name) },
                    leadingIcon = { ColorDot(tagColor(tag.colorHex)) },
                )
            }
        }

        OutlinedTextField(
            value = checkIn.comment,
            onValueChange = { onChange(checkIn.copy(comment = it)) },
            label = { Text("Comment (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}
