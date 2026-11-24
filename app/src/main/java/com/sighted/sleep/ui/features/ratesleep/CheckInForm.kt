package com.sighted.sleep.ui.features.ratesleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.model.CheckIn
import com.sighted.sleep.domain.model.Tag

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
        listOf(1..5, 6..10).forEach { range ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                range.forEach { n ->
                    val selected = checkIn.quality == n
                    Surface(
                        onClick = { onChange(checkIn.copy(quality = if (selected) null else n)) },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$n", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

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

@Composable
fun ColorDot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}

fun tagColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
