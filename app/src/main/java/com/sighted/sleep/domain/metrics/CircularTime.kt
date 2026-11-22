package com.sighted.sleep.domain.metrics

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Statistics for clock times, which wrap around midnight (spec §4).
 * Times are minutes after midnight (0..1439). The mean of 23:30 and 00:30 is 00:00, not 12:00.
 */
object CircularTime {
    const val DAY = 24 * 60

    private fun angle(minutes: Int) = 2 * PI * minutes / DAY

    /** Circular mean, or null for an empty list or when times cancel out exactly (e.g. 06:00 and 18:00). */
    fun mean(minutes: List<Int>): Int? {
        if (minutes.isEmpty()) return null
        val s = minutes.sumOf { sin(angle(it)) }
        val c = minutes.sumOf { cos(angle(it)) }
        if (sqrt(s * s + c * c) / minutes.size < 1e-9) return null
        val a = atan2(s, c).let { if (it < 0) it + 2 * PI else it }
        return (a / (2 * PI) * DAY).roundToInt() % DAY
    }

    /** Circular standard deviation in minutes: sqrt(-2 ln R), scaled to the 24h circle. Null if empty. */
    fun sd(minutes: List<Int>): Double? {
        if (minutes.isEmpty()) return null
        val s = minutes.sumOf { sin(angle(it)) } / minutes.size
        val c = minutes.sumOf { cos(angle(it)) } / minutes.size
        val r = sqrt(s * s + c * c).coerceIn(1e-12, 1.0)
        return sqrt(-2 * ln(r)) * DAY / (2 * PI)
    }

    /** Signed difference a − b in minutes, in (-720, 720]. */
    fun diff(a: Int, b: Int): Int {
        var d = ((a - b) % DAY + DAY) % DAY
        if (d > DAY / 2) d -= DAY
        return d
    }

    fun format(minutes: Int): String {
        val m = ((minutes % DAY) + DAY) % DAY
        return "%02d:%02d".format(m / 60, m % 60)
    }
}
