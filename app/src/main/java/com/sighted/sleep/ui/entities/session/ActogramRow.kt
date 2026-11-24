package com.sighted.sleep.ui.entities.session

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.metrics.CircularTime
import com.sighted.sleep.domain.metrics.DaySegment
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.Targets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** One actogram row: a calendar day with its sleep pieces (spec F5.2). */
data class ActogramRow(val date: LocalDate, val segments: List<DaySegment>, val isWorkDay: Boolean)

/** Rows run 15:00 (day before) → 15:00, keeping a typical night in the middle of one row. */
object ActogramAxis {
    /** Row start relative to midnight of the row's date, in minutes. */
    const val ROW_START_OFFSET_MIN = -9 * 60

    /** Clock time at the left edge of a row, in minutes after midnight (15:00). */
    const val ROW_START_CLOCK_MIN = 24 * 60 + ROW_START_OFFSET_MIN

    /** Width of the date label column. */
    val LABEL_WIDTH: Dp = 56.dp
}

/** Colours shared by the actogram rows and their legend. */
data class ActogramColors(val main: Color, val nap: Color, val band: Color, val track: Color, val grid: Color)

@Composable
fun actogramColors(): ActogramColors = ActogramColors(
    main = MaterialTheme.colorScheme.primary,
    nap = MaterialTheme.colorScheme.tertiary,
    band = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
    track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    grid = MaterialTheme.colorScheme.outlineVariant,
)

private val rowLabelFormat = DateTimeFormatter.ofPattern("EEE d")

/**
 * A day's sleep drawn on the 15:00 → 15:00 axis, with the target band and a 6-hour grid.
 * Free days get a tertiary date label. Figma: Entities / Sleep session / Actogram row.
 */
@Composable
fun ActogramDayRow(
    row: ActogramRow,
    targets: Targets,
    colors: ActogramColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.date.format(rowLabelFormat),
            style = MaterialTheme.typography.labelSmall,
            color = if (row.isWorkDay) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.width(ActogramAxis.LABEL_WIDTH),
        )
        Canvas(Modifier.weight(1f).height(18.dp)) {
            drawRow(row, targets, colors)
        }
    }
}

private fun DrawScope.drawRow(row: ActogramRow, targets: Targets, c: ActogramColors) {
    val w = size.width
    val h = size.height
    fun x(min: Int) = w * min / CircularTime.DAY

    // Clock time → minutes from the row's left edge (15:00).
    fun pos(clockMin: Int) = ((clockMin - ActogramAxis.ROW_START_CLOCK_MIN) % CircularTime.DAY + CircularTime.DAY) % CircularTime.DAY

    drawRect(c.track)
    // Target band. With 15:00-based rows a normal night (e.g. 23:00 → 07:00) is one block; wrap only if needed.
    val bed = pos(targets.bedtime.hour * 60 + targets.bedtime.minute)
    val wake = pos(targets.wake.hour * 60 + targets.wake.minute)
    if (bed < wake) {
        drawRect(c.band, Offset(x(bed), 0f), Size(x(wake) - x(bed), h))
    } else {
        drawRect(c.band, Offset(x(bed), 0f), Size(w - x(bed), h))
        drawRect(c.band, Offset(0f, 0f), Size(x(wake), h))
    }
    // 6-hour grid (21:00, 03:00, 09:00) plus a stronger midnight line.
    listOf(360, 720, 1080).forEach { drawLine(c.grid, Offset(x(it), 0f), Offset(x(it), h), strokeWidth = 1f) }
    drawLine(c.grid, Offset(x(pos(0)), 0f), Offset(x(pos(0)), h), strokeWidth = 3f)
    // Sleep segments
    row.segments.forEach { seg ->
        val color = if (seg.type == SessionType.MAIN) c.main else c.nap
        val left = x(seg.startMin)
        val width = (x(seg.endMin) - left).coerceAtLeast(2f)
        drawRoundRect(color, Offset(left, h * 0.15f), Size(width, h * 0.7f), CornerRadius(h * 0.2f))
    }
}
