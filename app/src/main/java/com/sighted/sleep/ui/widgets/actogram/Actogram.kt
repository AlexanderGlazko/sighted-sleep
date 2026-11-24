package com.sighted.sleep.ui.widgets.actogram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.metrics.CircularTime
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.entities.session.ActogramAxis
import com.sighted.sleep.ui.entities.session.ActogramDayRow
import com.sighted.sleep.ui.entities.session.ActogramRow
import com.sighted.sleep.ui.entities.session.actogramColors
import com.sighted.sleep.ui.kit.atoms.LegendItem

/**
 * The sleep timeline (spec F5.2): legend, hour axis (15 · 21 · 03 · 09 · 15) and one row per day,
 * newest first. Figma: the Legend + Actogram blocks on Timeline · Chart.
 */
@Composable
fun Actogram(
    rows: List<ActogramRow>,
    targets: Targets,
    onDayClick: (ActogramRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = actogramColors()
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(colors.main, "Main sleep")
            LegendItem(colors.nap, "Nap")
            LegendItem(colors.band, "Target")
        }
        Row(Modifier.padding(start = 16.dp + ActogramAxis.LABEL_WIDTH, end = 16.dp, top = 4.dp)) {
            // Labels every 6h from the row start: 15 · 21 · 03 · 09 · 15
            (0..4).map { CircularTime.format(ActogramAxis.ROW_START_CLOCK_MIN + it * 360).take(2) }.forEachIndexed { i, h ->
                Text(h, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (i < 4) Spacer(Modifier.weight(1f))
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(rows, key = { it.date.toEpochDay() }) { row ->
                ActogramDayRow(row, targets, colors, onClick = { onDayClick(row) })
            }
        }
    }
}
