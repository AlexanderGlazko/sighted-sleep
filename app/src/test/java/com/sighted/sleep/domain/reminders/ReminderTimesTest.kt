package com.sighted.sleep.domain.reminders

import com.sighted.sleep.domain.KYIV
import com.sighted.sleep.domain.at
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

class ReminderTimesTest {
    private fun z(s: String): ZonedDateTime = LocalDateTime.parse(s).atZone(KYIV)
    private val bed = LocalTime.of(23, 0)

    @Test
    fun `reminder later today`() {
        assertEquals(z("2026-11-22T22:30"), ReminderTimes.nextBedtimeReminder(z("2026-11-22T18:00"), bed, 30))
    }

    @Test
    fun `already passed rolls to tomorrow`() {
        assertEquals(z("2026-11-23T22:30"), ReminderTimes.nextBedtimeReminder(z("2026-11-22T22:30"), bed, 30))
        assertEquals(z("2026-11-23T22:30"), ReminderTimes.nextBedtimeReminder(z("2026-11-22T23:59"), bed, 30))
    }

    @Test
    fun `offset across midnight`() {
        // Bedtime 00:30, 60 min before → 23:30 the evening before.
        val half = LocalTime.of(0, 30)
        assertEquals(z("2026-11-22T23:30"), ReminderTimes.nextBedtimeReminder(z("2026-11-22T20:00"), half, 60))
        assertEquals(z("2026-11-23T23:30"), ReminderTimes.nextBedtimeReminder(z("2026-11-23T00:10"), half, 60))
    }

    @Test
    fun `wall clock time is kept across DST`() {
        // Kyiv falls back on 2026-10-25; the reminder stays at 22:30 local.
        val next = ReminderTimes.nextBedtimeReminder(z("2026-10-24T23:00"), bed, 30)
        assertEquals(z("2026-10-25T22:30"), next)
    }

    @Test
    fun `morning and still-sleeping times`() {
        assertEquals(at("2026-11-22T08:00"), ReminderTimes.morningReminderAt(at("2026-11-22T07:00"), 60))
        assertEquals(at("2026-11-22T13:00"), ReminderTimes.stillSleepingAt(at("2026-11-21T23:00")))
        assertEquals(Duration.ofHours(14), Duration.between(at("2026-11-21T23:00"), ReminderTimes.stillSleepingAt(at("2026-11-21T23:00"))))
    }
}
