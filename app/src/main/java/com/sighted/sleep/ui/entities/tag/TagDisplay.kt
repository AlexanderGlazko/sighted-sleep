package com.sighted.sleep.ui.entities.tag

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.ui.kit.atoms.ColorDot

/** A tag's stored colour (`#RRGGBB`), or grey if it can't be parsed. */
fun tagColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

/** Small coloured dot followed by the tag name (Figma: Entities / Tag). */
@Composable
fun TagLabel(tag: Tag, modifier: Modifier = Modifier, dotSize: Int = 8) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ColorDot(tagColor(tag.colorHex), size = dotSize)
        Text(" ${tag.name}", style = MaterialTheme.typography.bodySmall)
    }
}
