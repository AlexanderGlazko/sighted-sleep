package com.sighted.sleep.ui.features.tagfilter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.ui.entities.tag.tagColor
import com.sighted.sleep.ui.kit.atoms.ColorDot

/**
 * Filter the session list by one tag (spec F5.8). Tapping the selected tag clears the filter.
 * Figma: the Tag filter row on Timeline · List (Filter chip with a colour dot).
 */
@Composable
fun TagFilterRow(
    tags: List<Tag>,
    selectedTagId: Long?,
    onToggle: (tagId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onToggle(tag.id) },
                label = { Text(tag.name) },
                leadingIcon = { ColorDot(tagColor(tag.colorHex)) },
            )
        }
    }
}
