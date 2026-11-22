package com.sighted.sleep.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.domain.metrics.CircularTime
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.checkin.ColorDot
import com.sighted.sleep.ui.history.HistoryList
import java.time.format.DateTimeFormatter

private val rowLabelFormat = DateTimeFormatter.ofPattern("EEE d")
private val LABEL_WIDTH = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onEditSession: (Long) -> Unit,
    onAddSession: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: TimelineViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showList by rememberSaveable { mutableStateOf(false) }
    var dayPicker by remember { mutableStateOf<ActogramRow?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Timeline") }) },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSession) {
                Icon(Icons.Filled.Add, contentDescription = "Add sleep manually")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                listOf("Chart", "List").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = showList == (i == 1),
                        onClick = { showList = i == 1 },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(label) }
                }
            }
            when {
                showList -> HistoryList(onEditSession = onEditSession)
                state.loaded && state.rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sleep logged yet.", style = MaterialTheme.typography.bodyLarge)
                }
                else -> Actogram(state, onDayClick = { row ->
                    val ids = row.segments.map { it.sessionId }.distinct()
                    when (ids.size) {
                        0 -> Unit
                        1 -> onEditSession(ids.single())
                        else -> dayPicker = row
                    }
                })
            }
        }
    }

    dayPicker?.let { row ->
        AlertDialog(
            onDismissRequest = { dayPicker = null },
            title = { Text(row.date.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))) },
            text = {
                Column {
                    row.segments.distinctBy { it.sessionId }.forEach { seg ->
                        ListItem(
                            headlineContent = {
                                Text("${CircularTime.format(seg.startMin)} – ${CircularTime.format(seg.endMin)}")
                            },
                            supportingContent = { Text(if (seg.type == SessionType.MAIN) "Main sleep" else "Nap") },
                            modifier = Modifier.clickable { dayPicker = null; onEditSession(seg.sessionId) },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dayPicker = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun Actogram(state: TimelineUiState, onDayClick: (ActogramRow) -> Unit) {
    val main = MaterialTheme.colorScheme.primary
    val nap = MaterialTheme.colorScheme.tertiary
    val band = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val grid = MaterialTheme.colorScheme.outlineVariant

    Column(Modifier.fillMaxSize()) {
        // Legend
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(main, "Main sleep")
            LegendItem(nap, "Nap")
            LegendItem(band, "Target")
        }
        // Hour axis
        Row(Modifier.padding(start = 16.dp + LABEL_WIDTH, end = 16.dp, top = 4.dp)) {
            listOf("00", "06", "12", "18", "24").forEachIndexed { i, h ->
                Text(h, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (i < 4) Spacer(Modifier.weight(1f))
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)) {
            items(state.rows, key = { it.date.toEpochDay() }) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDayClick(row) }
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        row.date.format(rowLabelFormat),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (row.isWorkDay) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.width(LABEL_WIDTH),
                    )
                    Canvas(Modifier.weight(1f).height(18.dp)) {
                        drawRow(row, state.targets, main, nap, band, track, grid)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawRow(
    row: ActogramRow,
    targets: Targets,
    main: Color,
    nap: Color,
    band: Color,
    track: Color,
    grid: Color,
) {
    val w = size.width
    val h = size.height
    fun x(min: Int) = w * min / CircularTime.DAY

    drawRect(track)
    // Target band (may wrap past midnight, e.g. 23:00 → 07:00)
    val bed = targets.bedtime.hour * 60 + targets.bedtime.minute
    val wake = targets.wake.hour * 60 + targets.wake.minute
    if (bed < wake) {
        drawRect(band, Offset(x(bed), 0f), Size(x(wake) - x(bed), h))
    } else {
        drawRect(band, Offset(x(bed), 0f), Size(w - x(bed), h))
        drawRect(band, Offset(0f, 0f), Size(x(wake), h))
    }
    // 6-hour grid
    listOf(360, 720, 1080).forEach { drawLine(grid, Offset(x(it), 0f), Offset(x(it), h), strokeWidth = 1f) }
    // Sleep segments
    row.segments.forEach { seg ->
        val color = if (seg.type == SessionType.MAIN) main else nap
        val left = x(seg.startMin)
        val width = (x(seg.endMin) - left).coerceAtLeast(2f)
        drawRoundRect(color, Offset(left, h * 0.15f), Size(width, h * 0.7f), CornerRadius(h * 0.2f))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color, size = 10)
        Text(" $label", style = MaterialTheme.typography.labelSmall)
    }
}
