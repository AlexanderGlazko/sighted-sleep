package com.sighted.sleep.ui.widgets.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.metrics.Period
import com.sighted.sleep.domain.metrics.TrendPoint
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.kit.organisms.ChartCard
import com.sighted.sleep.ui.kit.organisms.ChartSeries
import com.sighted.sleep.ui.kit.organisms.RefLine
import com.sighted.sleep.ui.kit.organisms.SimpleChart
import com.sighted.sleep.ui.kit.organisms.hourTicks
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Trends for a Week / Month / Year (spec F5.3, F4.3): sleep per day with the target line,
 * timing (bedtime, midpoint, wake) and quality. Figma: Insights → Charts (Chart card).
 */
@Composable
fun TrendCharts(trend: List<TrendPoint>, period: Period, targets: Targets, modifier: Modifier = Modifier) {
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

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            ChartCard("Quality (1–10)") {
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
}

private fun LocalTime.toMinutes() = hour * 60 + minute
