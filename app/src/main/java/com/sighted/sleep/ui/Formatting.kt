package com.sighted.sleep.ui

import com.sighted.sleep.domain.TargetMath
import com.sighted.sleep.domain.ValidationError
import com.sighted.sleep.domain.formatDeviation
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val dayYearFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

fun SleepSession.startTimeText(): String = start.atZone(zone).format(timeFormat)
fun SleepSession.endTimeText(): String? = end?.atZone(zone)?.format(timeFormat)

/** "23:40 → 07:15", or "Since 23:40" for an active session. */
fun SleepSession.rangeText(): String =
    endTimeText()?.let { "${startTimeText()} → $it" } ?: "Since ${startTimeText()}"

fun LocalDate.dayText(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> format(if (year == today.year) dayFormat else dayYearFormat)
}

/** "Bedtime 47m late · Wake on time" relative to the target schedule (spec F4.2). */
fun SleepSession.deviationText(targets: Targets): String? {
    val endTime = end?.atZone(zone)?.toLocalTime() ?: return null
    val bed = TargetMath.deviationMinutes(start.atZone(zone).toLocalTime(), targets.bedtime)
    val wake = TargetMath.deviationMinutes(endTime, targets.wake)
    return "Bedtime ${formatDeviation(bed)} · Wake ${formatDeviation(wake)}"
}

fun SessionType.label(): String = when (this) {
    SessionType.MAIN -> "Main sleep"
    SessionType.NAP -> "Nap"
}

fun ValidationError.message(): String = when (this) {
    ValidationError.EndBeforeStart -> "Wake time must be after sleep time."
    ValidationError.StartInFuture -> "Sleep time can't be in the future."
    ValidationError.EndInFuture -> "Wake time can't be in the future."
    ValidationError.AnotherSessionActive -> "Another sleep is still running. Stop it first."
    is ValidationError.Overlaps ->
        "Overlaps another sleep (${other.endDate?.dayText() ?: "now"}, ${other.rangeText()})."
}

/** Signed duration for comparisons: "+25m", "−1h 10m", "±0m". */
fun signedDuration(minutes: Long): String = when {
    minutes > 0 -> "+" + com.sighted.sleep.domain.formatDuration(java.time.Duration.ofMinutes(minutes))
    minutes < 0 -> "−" + com.sighted.sleep.domain.formatDuration(java.time.Duration.ofMinutes(-minutes))
    else -> "±0m"
}
