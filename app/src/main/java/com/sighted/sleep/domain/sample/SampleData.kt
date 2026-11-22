package com.sighted.sleep.domain.sample

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

data class SampleSession(
    val start: Instant,
    val end: Instant,
    val quality: Int?,
    val tagNames: Set<String>,
)

/**
 * Realistic fake history for developing analytics (spec F7.5, debug builds only).
 * Weekday nights around 23:15 → 07:00, later weekends, a slow multi-week drift,
 * tag effects on bedtime and quality, and occasional weekend naps.
 * Deterministic for a given seed. Sessions never overlap and all end on or before [lastWakeDate].
 */
object SampleData {
    /** Stored as the session comment so sample data can be removed without touching real data. */
    const val MARKER = "[sample]"

    fun generate(lastWakeDate: LocalDate, days: Int, zone: ZoneId, seed: Long = 42): List<SampleSession> {
        val rnd = Random(seed)
        fun gauss(sd: Double): Double = (rnd.nextDouble() + rnd.nextDouble() + rnd.nextDouble() - 1.5) * 2 * sd
        fun chance(p: Double) = rnd.nextDouble() < p

        val out = mutableListOf<SampleSession>()
        for (i in 0 until days) {
            val wakeDate = lastWakeDate.minusDays((days - 1 - i).toLong())
            val free = wakeDate.dayOfWeek == DayOfWeek.SATURDAY || wakeDate.dayOfWeek == DayOfWeek.SUNDAY
            val drift = 40 * sin(i / 14.0 * PI) // slow schedule drift, ±40 min over ~4 weeks

            val tags = buildSet {
                if (chance(0.2)) add("Caffeine late")
                if (chance(0.35)) add("Screens in bed")
                if (chance(if (free) 0.25 else 0.08)) add("Alcohol")
                if (!free && chance(0.15)) add("Late work")
                if (chance(0.3)) add("Exercise")
                if (chance(0.2)) add("Stress")
                if (free && chance(0.35)) add("Social event")
            }

            var bedMin = (if (free) 24 * 60 + 45 else 23 * 60 + 15) + drift + gauss(25.0) // minutes after wakeDate-1 00:00
            if ("Caffeine late" in tags) bedMin += 40
            if ("Screens in bed" in tags) bedMin += 25
            if ("Late work" in tags) bedMin += 50
            if ("Social event" in tags) bedMin += 60
            if ("Exercise" in tags) bedMin -= 15
            bedMin = bedMin.coerceIn(21.0 * 60, 27.0 * 60) // 21:00 .. 03:00

            var wakeMin = (if (free) 9 * 60 + 30 else 7 * 60) + gauss(if (free) 40.0 else 10.0) + 24 * 60
            wakeMin = wakeMin.coerceIn(bedMin + 4.5 * 60, minOf(bedMin + 11 * 60, 24.0 * 60 + 12 * 60))

            val dayStart = wakeDate.minusDays(1).atStartOfDay(zone).toInstant()
            val start = dayStart.plusSeconds((bedMin * 60).roundToLong())
            val end = dayStart.plusSeconds((wakeMin * 60).roundToLong())
            val hours = (wakeMin - bedMin) / 60

            var q = 5 + (hours - 7) * 1.2 + gauss(1.0)
            if ("Caffeine late" in tags) q -= 1.5
            if ("Alcohol" in tags) q -= 1.5
            if ("Stress" in tags) q -= 1.0
            if ("Exercise" in tags) q += 0.8
            val quality = if (chance(0.9)) q.roundToInt().coerceIn(1, 10) else null // some nights unrated

            out += SampleSession(start, end, quality, tags)

            if (free && chance(0.3)) {
                val napStart = wakeDate.atStartOfDay(zone).toInstant().plusSeconds(((14 * 60 + rnd.nextInt(90)) * 60).toLong())
                out += SampleSession(napStart, napStart.plusSeconds((20 + rnd.nextInt(40)) * 60L), null, emptySet())
            }
        }
        return out
    }
}
