package com.sighted.sleep.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.domain.formatDuration
import com.sighted.sleep.domain.metrics.BedtimeBucket
import com.sighted.sleep.domain.metrics.CircularTime
import com.sighted.sleep.domain.metrics.InsightMetrics
import com.sighted.sleep.domain.metrics.MIN_SAMPLES
import com.sighted.sleep.domain.metrics.TagImpact
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.ui.checkin.ColorDot
import com.sighted.sleep.ui.checkin.tagColor
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt

/** "When is my best time to fall asleep?" (spec F5.5) */
@Composable
fun BestBedtimeCard(buckets: List<BedtimeBucket>, best: BedtimeBucket?) {
    InsightCard("Quality by bedtime") {
        if (buckets.isEmpty()) {
            Hint("Rate a few nights to see which bedtime suits you best.")
            return@InsightCard
        }
        Text(
            if (best != null) {
                "Best bedtime: ${range(best.startMin)} · avg %.1f/10 over ${best.count} nights".format(best.avgQuality)
            } else {
                "Not enough data to pick a best time yet (needs $MIN_SAMPLES+ rated nights in one half-hour)."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        val enough = MaterialTheme.colorScheme.primary
        val few = MaterialTheme.colorScheme.outline
        SimpleChart(
            xLabels = buckets.map { CircularTime.format(it.startMin) },
            series = listOf(
                ChartSeries(enough, buckets.map { b -> b.avgQuality.takeIf { b.enoughData } }),
                ChartSeries(few, buckets.map { b -> b.avgQuality.takeIf { !b.enoughData } }),
            ),
            yMin = 0.0, yMax = 10.0,
            yLabel = { it.roundToInt().toString() },
            yTicks = listOf(0.0, 2.0, 4.0, 6.0, 8.0, 10.0),
            bars = true,
        )
        Hint("Average quality by the half-hour you fell asleep. Grey bars have fewer than $MIN_SAMPLES nights.")
    }
}

/** "What does late caffeine do to me?" (spec F5.6) */
@Composable
fun TagImpactCard(impacts: List<Pair<Tag, TagImpact>>) {
    InsightCard("Tag impact") {
        if (impacts.isEmpty()) {
            Hint("Tag a few nights in the morning check-in to see their effect.")
            return@InsightCard
        }
        impacts.forEachIndexed { i, (tag, impact) ->
            if (i > 0) HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(tagColor(tag.colorHex), size = 10)
                    Text("  ${tag.name}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        "${impact.withCount} with · ${impact.withoutCount} without",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val parts = listOfNotNull(
                    impact.qualityDelta?.let { "Quality %+.1f".format(it) },
                    impact.durationDeltaMin?.let { "Sleep ${signed(it)}" },
                    impact.midpointShiftMin?.let {
                        if (abs(it) < 5) "Midpoint same" else
                            "Midpoint ${formatDuration(Duration.ofMinutes(abs(it).toLong()))} ${if (it > 0) "later" else "earlier"}"
                    },
                )
                Text(
                    parts.joinToString(" · ").ifEmpty { "Not enough data yet ($MIN_SAMPLES+ nights with and without)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (parts.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Hint("Main sleeps with vs. without each tag in this period. These are patterns, not proof of cause.")
    }
}

@Composable
private fun InsightCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun range(start: Int) = "${CircularTime.format(start)}–${CircularTime.format(start + InsightMetrics.BUCKET)}"

private fun signed(min: Int) = when {
    min > 0 -> "+" + formatDuration(Duration.ofMinutes(min.toLong()))
    min < 0 -> "−" + formatDuration(Duration.ofMinutes(-min.toLong()))
    else -> "±0m"
}
