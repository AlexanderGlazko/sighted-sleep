package com.sighted.sleep.ui.kit.atoms

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Coloured dot and a small label for chart legends (Figma: Atoms / Legend item). */
@Composable
fun LegendItem(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color, size = 10)
        Text(" $label", style = MaterialTheme.typography.labelSmall)
    }
}
