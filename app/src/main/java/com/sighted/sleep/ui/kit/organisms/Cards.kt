package com.sighted.sleep.ui.kit.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sighted.sleep.ui.kit.atoms.ColorDot

/**
 * Small metric card: label, big value, an optional supporting line and an optional comparison line
 * in the primary colour (Figma: Organisms / Stat card).
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
    comparison: String? = null,
) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
            if (supporting.isNotEmpty()) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            comparison?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * Full-width card with a title, an optional legend row and any content, usually a [SimpleChart]
 * (Figma: Organisms / Chart card).
 */
@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    legend: List<Pair<Color, String>> = emptyList(),
    content: @Composable () -> Unit,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (legend.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    legend.forEach { (c, l) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(c, size = 8)
                            Text(" $l", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            content()
        }
    }
}
