package com.sighted.sleep.domain.reminders

import com.sighted.sleep.domain.model.SessionRules
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

/** When each reminder should fire (spec F6, §3.6). Pure functions, DST-safe via ZonedDateTime. */
object ReminderTimes {

    /**
     * Next bedtime reminder strictly after [now]: [offsetMinutes] before the target bedtime.
     * Handles offsets that cross midnight (bedtime 00:30 − 60 min → 23:30 the evening before).
     */
    fun nextBedtimeReminder(now: ZonedDateTime, bedtime: LocalTime, offsetMinutes: Int): ZonedDateTime {
        val today = now.toLocalDate()
        return (-1L..2L)
            .map { today.plusDays(it).atTime(bedtime).atZone(now.zone).minusMinutes(offsetMinutes.toLong()) }
            .filter { it.isAfter(now) }
            .min()
    }

    /** Morning check-in reminder: [delayMinutes] after waking (spec F6.2). */
    fun morningReminderAt(wake: Instant, delayMinutes: Int): Instant = wake.plusSeconds(delayMinutes * 60L)

    /** "Still sleeping?" alert for an active session (spec §3.6). */
    fun stillSleepingAt(start: Instant): Instant = start.plus(SessionRules.STILL_SLEEPING_AFTER)
}
