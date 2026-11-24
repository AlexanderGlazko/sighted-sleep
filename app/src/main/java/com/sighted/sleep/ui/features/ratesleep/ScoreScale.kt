package com.sighted.sleep.ui.features.ratesleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Quality score 1–10 in two rows of five (spec F2.1). Figma: Features / Rate sleep / Score scale.
 * Tapping the selected value again clears it.
 */
@Composable
fun ScoreScale(
    value: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(1..5, 6..10).forEach { range ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                range.forEach { n ->
                    val selected = value == n
                    Surface(
                        onClick = { onChange(if (selected) null else n) },
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
    }
}
