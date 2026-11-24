package com.sighted.sleep.ui.kit.organisms

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

/** A line on a chart; null values leave a gap. */
data class ChartSeries(val color: Color, val values: List<Double?>)

/** A dashed horizontal reference line, e.g. the target. */
data class RefLine(val value: Double, val color: Color)

/**
 * Small dependency-free chart (bars or lines) drawn on a Canvas.
 * Custom-drawn instead of a chart library so it can be styled freely in the design pass.
 */
@Composable
fun SimpleChart(
    xLabels: List<String>,
    series: List<ChartSeries>,
    yMin: Double,
    yMax: Double,
    yLabel: (Double) -> String,
    yTicks: List<Double>,
    bars: Boolean = false,
    refLines: List<RefLine> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier.fillMaxWidth().height(180.dp)) {
        val left = 44.dp.toPx()
        val bottom = 20.dp.toPx()
        val top = 6.dp.toPx()
        val plotW = size.width - left
        val plotH = size.height - bottom - top
        val n = xLabels.size.coerceAtLeast(1)
        val slot = plotW / n
        fun x(i: Int) = left + slot * (i + 0.5f)
        fun y(v: Double) = top + plotH * (1 - ((v - yMin) / (yMax - yMin)).toFloat().coerceIn(0f, 1f))

        // Y grid + labels
        yTicks.forEach { t ->
            drawLine(grid, Offset(left, y(t)), Offset(size.width, y(t)), strokeWidth = 1f)
            drawLabel(measurer, yLabel(t), labelStyle, Offset(0f, y(t) - 7.dp.toPx()))
        }
        // X labels: at most ~7 so they never collide
        val every = ceil(n / 7.0).toInt().coerceAtLeast(1)
        xLabels.forEachIndexed { i, l ->
            if (i % every == 0) drawLabel(measurer, l, labelStyle, Offset(x(i) - 10.dp.toPx(), size.height - bottom + 4.dp.toPx()))
        }
        refLines.forEach { r ->
            drawLine(
                r.color, Offset(left, y(r.value)), Offset(size.width, y(r.value)), strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
        series.forEach { s ->
            if (bars) {
                // Cap the width so one or two bars don't turn into wide blobs.
                val w = (slot * 0.6f).coerceIn(2f, 28.dp.toPx())
                s.values.forEachIndexed { i, v ->
                    if (v != null) {
                        val yTop = y(v)
                        drawRoundRect(s.color, Offset(x(i) - w / 2, yTop), Size(w, y(yMin) - yTop), CornerRadius(w / 4))
                    }
                }
            } else {
                var prev: Offset? = null
                s.values.forEachIndexed { i, v ->
                    if (v == null) { prev = null; return@forEachIndexed }
                    val p = Offset(x(i), y(v))
                    prev?.let { drawLine(s.color, it, p, strokeWidth = 3f) }
                    drawCircle(s.color, radius = if (n > 40) 2.5f else 5f, center = p)
                    prev = p
                }
            }
        }
    }
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, style: TextStyle, at: Offset) {
    drawText(measurer, text, topLeft = at, style = style)
}

/** Whole-hour ticks covering [min, max] (values in hours), at most ~5 lines. */
fun hourTicks(min: Double, max: Double): List<Double> {
    val lo = floor(min)
    val hi = ceil(max)
    val step = ceil((hi - lo) / 4).coerceAtLeast(1.0)
    return generateSequence(lo) { it + step }.takeWhile { it <= hi + 1e-9 }.toList()
}
