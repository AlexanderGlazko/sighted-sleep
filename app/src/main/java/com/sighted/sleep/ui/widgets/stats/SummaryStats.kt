package com.sighted.sleep.ui.widgets.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.formatDeviation
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.metrics.CircularTime
import com.sighted.sleep.domain.metrics.Period
import com.sighted.sleep.domain.metrics.PeriodSummary
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.kit.organisms.StatCard
import com.sighted.sleep.ui.signedDuration
import java.time.Duration
import java.time.LocalTime
import kotlin.math.abs

private data class Stat(val title: String, val value: String, val sub: String, val compare: String? = null)

/**
 * The period's summary metrics as a two-column grid of stat cards, each compared with the previous
 * period (spec F5.1, F5.4, F5.7). Figma: the Stats block on Insights (8 × Stat card).
 * Consistency and social jetlag are left out for a single day.
 */
@Composable
fun SummaryStats(
    summary: PeriodSummary,
    previous: PeriodSummary?,
    targets: Targets,
    period: Period,
    modifier: Modifier = Modifier,
) {
    val s = summary
    val prev = previous
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
        if (abs(it) < 5) "same" else "${formatDuration(Duration.ofMinutes(abs(it).toLong()))} ${if (it > 0) "later" else "earlier"}"
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { StatCard(it.title, it.value, it.sub, Modifier.weight(1f), comparison = it.compare) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun LocalTime.toMinutes() = hour * 60 + minute
