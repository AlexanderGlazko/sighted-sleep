package com.sighted.sleep.ui.pages.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.domain.formatDeviation
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.metrics.CircularTime
import com.sighted.sleep.domain.metrics.Period
import com.sighted.sleep.domain.metrics.PeriodSummary
import com.sighted.sleep.domain.metrics.TrendPoint
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.features.ratesleep.ColorDot
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    bottomBar: @Composable () -> Unit,
    viewModel: InsightsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Insights") }) }, bottomBar = bottomBar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val periods = listOf(Period.DAY to "Day", Period.WEEK to "Week", Period.MONTH to "Month", Period.YEAR to "Year")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                periods.forEachIndexed { i, (p, label) ->
                    SegmentedButton(
                        selected = state.period == p,
                        onClick = { viewModel.setPeriod(p) },
                        shape = SegmentedButtonDefaults.itemShape(i, periods.size),
                    ) { Text(label) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previous) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                }
                Text(
                    state.label,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = viewModel::next, enabled = state.canGoNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                }
            }

            val summary = state.summary ?: return@Column
            if (summary.loggedDays == 0) {
                Text(
                    "No sleep logged in this period.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            SummaryCards(summary, state.previous, state.targets, state.period)

            if (state.period != Period.DAY && state.trend.isNotEmpty()) {
                TrendCharts(state.trend, state.period, state.targets)
            }

            if (state.period != Period.DAY) {
                BestBedtimeCard(state.bedtimeBuckets, state.bestBedtime)
                TagImpactCard(state.tagImpacts)
            }
        }
    }
}

@Composable
private fun SummaryCards(s: PeriodSummary, prev: PeriodSummary?, targets: Targets, period: Period) {
    val bedTarget = targets.bedtime.toMinutes()
    val wakeTarget = targets.wake.toMinutes()
    val vs = when (period) {
        Period.DAY -> "the day before"
        Period.WEEK -> "previous week"
        Period.MONTH -> "previous month"
        Period.YEAR -> "previous year"
    }
    // "+25m vs previous week" etc. (spec F5.7); null when either side has no value.
    fun durDelta(a: Duration?, b: Duration?) = if (a != null && b != null) signedDuration(a.toMinutes() - b.toMinutes()) else null
    fun clockDelta(a: Int?, b: Int?) = if (a != null && b != null) CircularTime.diff(a, b).let {
        if (kotlin.math.abs(it) < 5) "same" else "${formatDuration(Duration.ofMinutes(kotlin.math.abs(it).toLong()))} ${if (it > 0) "later" else "earlier"}"
    } else null
    fun cmp(text: String?) = text?.let { "$it vs $vs" }

    val cards = buildList {
        add(Stat(if (period == Period.DAY) "Sleep" else "Avg sleep / day", s.avgDailyTotal?.let(::formatDuration) ?: "–",
            "Target ${formatDuration(targets.duration)}", cmp(durDelta(s.avgDailyTotal, prev?.avgDailyTotal))))
        add(Stat("Sleep debt (7 days)", formatDuration(s.sleepDebt), if (s.sleepDebt.isZero) "None" else "Below target"))
        add(Stat("Bedtime", s.avgBedtime?.let(CircularTime::format) ?: "–",
            s.avgBedtime?.let { formatDeviation(CircularTime.diff(it, bedTarget)) } ?: "", cmp(clockDelta(s.avgBedtime, prev?.avgBedtime))))
        add(Stat("Wake time", s.avgWake?.let(CircularTime::format) ?: "–",
            s.avgWake?.let { formatDeviation(CircularTime.diff(it, wakeTarget)) } ?: "", cmp(clockDelta(s.avgWake, prev?.avgWake))))
        add(Stat("Sleep midpoint", s.avgMidpoint?.let(CircularTime::format) ?: "–", "Body-clock marker",
            cmp(clockDelta(s.avgMidpoint, prev?.avgMidpoint))))
        add(Stat("Quality", s.avgQuality?.let { "%.1f/10".format(it) } ?: "–", if (s.avgQuality == null) "Not rated" else "Average",
            cmp(if (s.avgQuality != null && prev?.avgQuality != null) "%+.1f".format(s.avgQuality - prev.avgQuality) else null)))
        if (period != Period.DAY) {
            add(Stat("Consistency", s.consistency?.let { "$it/100" } ?: "–", if (s.consistency == null) "Needs 3+ nights" else "Bed & wake regularity",
                cmp(if (s.consistency != null && prev?.consistency != null) "%+d".format(s.consistency - prev.consistency) else null)))
            add(Stat("Social jetlag", s.socialJetlagMinutes?.let { formatDuration(Duration.ofMinutes(it.toLong())) } ?: "–",
                when (s.socialJetlagMinutes) {
                    null -> "Needs work & free days"
                    in 0..60 -> "OK (≤ 1h)"
                    else -> "High (> 1h)"
                },
                cmp(if (s.socialJetlagMinutes != null && prev?.socialJetlagMinutes != null)
                    signedDuration((s.socialJetlagMinutes - prev.socialJetlagMinutes).toLong()) else null)))
        }
    }
    cards.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { StatCard(it, Modifier.weight(1f)) }
            if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        }
    }
}

private data class Stat(val title: String, val value: String, val sub: String, val compare: String? = null)

/** "+25m" / "−1h 10m" / "±0m". */
private fun signedDuration(minutes: Long): String = when {
    minutes > 0 -> "+" + formatDuration(Duration.ofMinutes(minutes))
    minutes < 0 -> "−" + formatDuration(Duration.ofMinutes(-minutes))
    else -> "±0m"
}

@Composable
private fun StatCard(stat: Stat, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stat.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stat.value, style = MaterialTheme.typography.titleLarge)
            if (stat.sub.isNotEmpty()) {
                Text(stat.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            stat.compare?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TrendCharts(trend: List<TrendPoint>, period: Period, targets: Targets) {
    val labels = trend.map {
        when (period) {
            Period.WEEK -> it.start.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
            Period.MONTH -> it.start.dayOfMonth.toString()
            else -> it.start.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.outline
    // Timing lines need clearly different hues; theme roles are too similar in dark mode.
    val tertiary = Color(0xFFBA68C8) // bedtime
    val secondary = Color(0xFFFFB74D) // midpoint

    // Sleep per day
    val targetHours = targets.duration.toMinutes() / 60.0
    val hours = trend.map { it.totalHours }
    val maxH = maxOf(targetHours, hours.filterNotNull().maxOrNull() ?: 0.0) + 1
    ChartCard("Sleep per day (hours)", legend = listOf(primary to "Sleep", targetColor to "Target")) {
        SimpleChart(
            xLabels = labels,
            series = listOf(ChartSeries(primary, hours)),
            yMin = 0.0, yMax = maxH,
            yLabel = { "${it.roundToInt()}h" },
            yTicks = hourTicks(0.0, maxH),
            bars = true,
            refLines = listOf(RefLine(targetHours, targetColor)),
        )
    }

    // Timing: clock times unwrapped around noon so a night reads 22 → 24 → 31 without a jump at midnight.
    fun unwrap(min: Int?) = min?.let { (((it - 720 + 1440) % 1440) + 720) / 60.0 }
    val bed = trend.map { unwrap(it.bedtime) }
    val mid = trend.map { unwrap(it.midpoint) }
    val wake = trend.map { unwrap(it.wake) }
    val all = (bed + mid + wake).filterNotNull() + listOfNotNull(unwrap(targets.bedtime.toMinutes()), unwrap(targets.wake.toMinutes()))
    if (bed.any { it != null }) {
        val lo = floor(all.min() - 0.5)
        val hi = ceil(all.max() + 0.5)
        ChartCard("Timing", legend = listOf(tertiary to "Bedtime", secondary to "Midpoint", primary to "Wake", targetColor to "Target")) {
            SimpleChart(
                xLabels = labels,
                series = listOf(ChartSeries(tertiary, bed), ChartSeries(secondary, mid), ChartSeries(primary, wake)),
                yMin = lo, yMax = hi,
                yLabel = { "%02d:00".format(it.roundToInt() % 24) },
                yTicks = hourTicks(lo, hi),
                refLines = listOfNotNull(
                    unwrap(targets.bedtime.toMinutes())?.let { RefLine(it, targetColor) },
                    unwrap(targets.wake.toMinutes())?.let { RefLine(it, targetColor) },
                ),
            )
        }
    }

    val quality = trend.map { it.quality }
    if (quality.any { it != null }) {
        ChartCard("Quality (1–10)", legend = emptyList()) {
            SimpleChart(
                xLabels = labels,
                series = listOf(ChartSeries(primary, quality)),
                yMin = 1.0, yMax = 10.0,
                yLabel = { it.roundToInt().toString() },
                yTicks = listOf(2.0, 4.0, 6.0, 8.0, 10.0),
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, legend: List<Pair<Color, String>>, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
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

private fun LocalTime.toMinutes() = hour * 60 + minute
private fun floor(v: Double) = kotlin.math.floor(v)
private fun ceil(v: Double) = kotlin.math.ceil(v)
