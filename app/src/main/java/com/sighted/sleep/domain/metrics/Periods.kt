package com.sighted.sleep.domain.metrics

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class Period { DAY, WEEK, MONTH, YEAR }

/** Inclusive date range. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    val days: List<LocalDate>
        get() = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
}

object Periods {
    /** The period containing [anchor]. Weeks run Monday–Sunday (spec §4). */
    fun range(period: Period, anchor: LocalDate): DateRange = when (period) {
        Period.DAY -> DateRange(anchor, anchor)
        Period.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { DateRange(it, it.plusDays(6)) }
        Period.MONTH -> DateRange(anchor.withDayOfMonth(1), anchor.with(TemporalAdjusters.lastDayOfMonth()))
        Period.YEAR -> DateRange(anchor.withDayOfYear(1), anchor.with(TemporalAdjusters.lastDayOfYear()))
    }

    /** Moves the anchor by [steps] whole periods (negative = back in time). */
    fun shift(period: Period, anchor: LocalDate, steps: Long): LocalDate = when (period) {
        Period.DAY -> anchor.plusDays(steps)
        Period.WEEK -> anchor.plusWeeks(steps)
        Period.MONTH -> anchor.plusMonths(steps)
        Period.YEAR -> anchor.plusYears(steps)
    }
}

/** Aggregates for one period (spec §4 metrics table). Null = not enough data. */
data class PeriodSummary(
    val range: DateRange,
    val loggedDays: Int,
    val avgDailyTotal: Duration?,
    val avgBedtime: Int?,
    val avgWake: Int?,
    val avgMidpoint: Int?,
    val avgQuality: Double?,
    val consistency: Int?,
    val socialJetlagMinutes: Int?,
    /** 7-day debt at the period's last day (or today, if the period is still running). */
    val sleepDebt: Duration,
)

/** One point on a trend chart. Clock times are minutes after midnight. */
data class TrendPoint(
    val start: LocalDate,
    val totalHours: Double?,
    val bedtime: Int?,
    val wake: Int?,
    val midpoint: Int?,
    val quality: Double?,
)

object PeriodStats {

    fun summary(
        range: DateRange,
        mains: List<MainSleep>,
        totals: Map<LocalDate, Duration>,
        target: Duration,
        today: LocalDate,
    ): PeriodSummary {
        val inRange = mains.filter { it.date in range }
        val dayTotals = totals.filterKeys { it in range }.values
        val quality = inRange.mapNotNull { it.quality }
        return PeriodSummary(
            range = range,
            loggedDays = dayTotals.size,
            avgDailyTotal = if (dayTotals.isEmpty()) null else dayTotals.reduce(Duration::plus).dividedBy(dayTotals.size.toLong()),
            avgBedtime = CircularTime.mean(inRange.map { it.bedtime }),
            avgWake = CircularTime.mean(inRange.map { it.wake }),
            avgMidpoint = CircularTime.mean(inRange.map { it.midpoint }),
            avgQuality = quality.takeIf { it.isNotEmpty() }?.average(),
            consistency = SleepMetrics.consistencyScore(inRange),
            socialJetlagMinutes = SleepMetrics.socialJetlagMinutes(inRange),
            sleepDebt = SleepMetrics.sleepDebt(totals, target, minOf(range.end, today)),
        )
    }

    /**
     * Trend points: one per day for Week/Month, one per Monday-starting week for Year (spec §4 periods).
     * Day has no trend.
     */
    fun trend(
        period: Period,
        range: DateRange,
        mains: List<MainSleep>,
        totals: Map<LocalDate, Duration>,
    ): List<TrendPoint> {
        val buckets: List<DateRange> = when (period) {
            Period.DAY -> return emptyList()
            Period.WEEK, Period.MONTH -> range.days.map { DateRange(it, it) }
            Period.YEAR -> {
                val firstMonday = range.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                generateSequence(firstMonday) { it.plusWeeks(1) }
                    .takeWhile { !it.isAfter(range.end) }
                    .map { DateRange(maxOf(it, range.start), minOf(it.plusDays(6), range.end)) }
                    .toList()
            }
        }
        return buckets.map { bucket ->
            val m = mains.filter { it.date in bucket }
            val t = totals.filterKeys { it in bucket }.values
            val q = m.mapNotNull { it.quality }
            TrendPoint(
                start = bucket.start,
                totalHours = if (t.isEmpty()) null else t.sumOf { it.toMinutes() } / 60.0 / t.size,
                bedtime = CircularTime.mean(m.map { it.bedtime }),
                wake = CircularTime.mean(m.map { it.wake }),
                midpoint = CircularTime.mean(m.map { it.midpoint }),
                quality = q.takeIf { it.isNotEmpty() }?.average(),
            )
        }
    }
}
