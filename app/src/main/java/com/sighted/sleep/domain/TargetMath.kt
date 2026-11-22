package com.sighted.sleep.domain

import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.model.Targets
import java.time.LocalDateTime
import java.time.LocalTime

object TargetMath {
    private const val DAY_MINUTES = 24 * 60

    /**
     * Signed circular difference between a clock time and its target, in minutes, in (-720, 720].
     * Positive = later than target. Example: actual 00:30, target 23:00 → +90 (not -1350).
     */
    fun deviationMinutes(actual: LocalTime, target: LocalTime): Int {
        val raw = (actual.toSecondOfDay() / 60) - (target.toSecondOfDay() / 60)
        var d = ((raw % DAY_MINUTES) + DAY_MINUTES) % DAY_MINUTES // 0..1439
        if (d > DAY_MINUTES / 2) d -= DAY_MINUTES
        return d
    }

    /**
     * Default range for "Add sleep manually": the most recent target night that has already ended.
     * If it's before today's target wake time, that's last night ending yesterday morning.
     */
    fun defaultManualRange(now: LocalDateTime, targets: Targets): Pair<LocalDateTime, LocalDateTime> {
        val wakeDate = if (now.toLocalTime() >= targets.wake) now.toLocalDate() else now.toLocalDate().minusDays(1)
        val end = wakeDate.atTime(targets.wake)
        return end.minus(targets.duration) to end
    }
}

/** The most recent finished main sleep, plus finished naps that ended after it. */
data class LatestSleep(val main: SleepSession?, val napsAfter: List<SleepSession>)

fun latestSleep(sessions: List<SleepSession>, types: Map<Long, SessionType>): LatestSleep {
    val finished = sessions.filter { !it.isActive }.sortedByDescending { it.end }
    val main = finished.firstOrNull { types[it.id] == SessionType.MAIN }
    val naps = finished.filter { s ->
        types[s.id] == SessionType.NAP && (main == null || s.end!! > main.end)
    }
    return LatestSleep(main, naps)
}
