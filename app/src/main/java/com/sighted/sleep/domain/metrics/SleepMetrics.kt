package com.sighted.sleep.domain.metrics

import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/** A finished main sleep with the derived values the metrics use (spec §4). */
data class MainSleep(
    val session: SleepSession,
    /** Calendar day it's attributed to: the day it ended (spec §3.3). */
    val date: LocalDate,
    val bedtime: Int,
    val wake: Int,
    val midpoint: Int,
    val duration: Duration,
    val quality: Int?,
    /** Sleep before a free day (spec §3.4). */
    val isFreeDay: Boolean,
    val tagIds: Set<Long>,
)

/** A piece of a session inside one calendar day, for the actogram. Minutes after local midnight. */
data class DaySegment(val sessionId: Long, val startMin: Int, val endMin: Int, val type: SessionType)

object SleepMetrics {

    private fun minutesOfDay(instant: Instant, s: SleepSession): Int =
        instant.atZone(s.zone).toLocalTime().let { it.hour * 60 + it.minute }

    fun mainSleeps(
        sessions: List<SleepSession>,
        types: Map<Long, SessionType>,
        sessionTags: Map<Long, Set<Long>>,
        workDays: Set<DayOfWeek>,
        dayOffTagId: Long?,
    ): List<MainSleep> = sessions
        .filter { !it.isActive && types[it.id] == SessionType.MAIN }
        .map { s ->
            val end = s.end!!
            val duration = s.duration(end)
            val tags = sessionTags[s.id].orEmpty()
            val date = s.endDate!!
            MainSleep(
                session = s,
                date = date,
                bedtime = minutesOfDay(s.start, s),
                wake = minutesOfDay(end, s),
                midpoint = minutesOfDay(s.start.plus(duration.dividedBy(2)), s),
                duration = duration,
                quality = s.quality,
                isFreeDay = date.dayOfWeek !in workDays || (dayOffTagId != null && dayOffTagId in tags),
                tagIds = tags,
            )
        }
        .sortedBy { it.date }

    /**
     * Total sleep per calendar day, splitting sessions at local midnight (spec §3.3).
     * Only finished sessions count. Days with no sleep are absent from the map.
     */
    fun dailyTotals(sessions: List<SleepSession>): Map<LocalDate, Duration> {
        val totals = mutableMapOf<LocalDate, Duration>()
        sessions.filter { !it.isActive }.forEach { s ->
            val end = s.end!!
            var day = s.start.atZone(s.zone).toLocalDate()
            while (true) {
                val dayStart = day.atStartOfDay(s.zone).toInstant()
                val dayEnd = day.plusDays(1).atStartOfDay(s.zone).toInstant()
                if (!dayStart.isBefore(end)) break
                val from = maxOf(s.start, dayStart)
                val to = minOf(end, dayEnd)
                if (to.isAfter(from)) totals.merge(day, Duration.between(from, to), Duration::plus)
                day = day.plusDays(1)
            }
        }
        return totals
    }

    /** Sessions cut into per-day pieces for the actogram, keyed by local date. */
    fun daySegments(sessions: List<SleepSession>, types: Map<Long, SessionType>, now: Instant): Map<LocalDate, List<DaySegment>> {
        val rows = mutableMapOf<LocalDate, MutableList<DaySegment>>()
        sessions.forEach { s ->
            val end = s.end ?: now
            val type = types[s.id] ?: SessionType.MAIN
            var day = s.start.atZone(s.zone).toLocalDate()
            while (true) {
                val dayStart = day.atStartOfDay(s.zone).toInstant()
                val dayEnd = day.plusDays(1).atStartOfDay(s.zone).toInstant()
                if (!dayStart.isBefore(end)) break
                val from = maxOf(s.start, dayStart)
                val to = minOf(end, dayEnd)
                if (to.isAfter(from)) {
                    val startMin = Duration.between(dayStart, from).toMinutes().toInt()
                    val endMin = Duration.between(dayStart, to).toMinutes().toInt().coerceAtMost(CircularTime.DAY)
                    rows.getOrPut(day) { mutableListOf() }.add(DaySegment(s.id, startMin, endMin, type))
                }
                day = day.plusDays(1)
            }
        }
        return rows
    }

    /**
     * Consistency score 0–100 (spec §4): 100 − avgSD × 100 / 120, where avgSD is the mean of the
     * circular SDs of bedtime and wake time. Needs at least 3 main sleeps.
     */
    fun consistencyScore(mains: List<MainSleep>): Int? {
        if (mains.size < 3) return null
        val avgSd = (CircularTime.sd(mains.map { it.bedtime })!! + CircularTime.sd(mains.map { it.wake })!!) / 2
        return (100 - avgSd * 100 / 120).coerceIn(0.0, 100.0).roundToInt()
    }

    /** Social jetlag in minutes: |mean midpoint (free days) − mean midpoint (work days)|. Needs both. */
    fun socialJetlagMinutes(mains: List<MainSleep>): Int? {
        val free = CircularTime.mean(mains.filter { it.isFreeDay }.map { it.midpoint }) ?: return null
        val work = CircularTime.mean(mains.filter { !it.isFreeDay }.map { it.midpoint }) ?: return null
        return kotlin.math.abs(CircularTime.diff(free, work))
    }

    /**
     * Sleep debt over the 7 days ending on [endDate] (spec §4): sum of (target − daily total) over days
     * that have any logged sleep. Surplus offsets deficit, but debt never goes below zero.
     * Days with nothing logged are skipped: missing data isn't counted as zero sleep.
     */
    fun sleepDebt(totals: Map<LocalDate, Duration>, target: Duration, endDate: LocalDate, days: Int = 7): Duration {
        var sum = Duration.ZERO
        for (i in 0 until days) {
            val total = totals[endDate.minusDays(i.toLong())] ?: continue
            sum = sum.plus(target.minus(total))
        }
        return if (sum.isNegative) Duration.ZERO else sum
    }
}
