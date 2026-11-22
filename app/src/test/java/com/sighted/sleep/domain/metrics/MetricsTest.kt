package com.sighted.sleep.domain.metrics

import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.at
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

class CircularTimeTest {
    private fun m(s: String) = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    @Test
    fun `mean wraps around midnight`() {
        assertEquals(m("00:00"), CircularTime.mean(listOf(m("23:30"), m("00:30"))))
        assertEquals(m("23:00"), CircularTime.mean(listOf(m("22:00"), m("23:00"), m("00:00"))))
        assertEquals(m("07:00"), CircularTime.mean(listOf(m("07:00"))))
    }

    @Test
    fun `mean is null for empty or opposite times`() {
        assertNull(CircularTime.mean(emptyList()))
        assertNull(CircularTime.mean(listOf(m("06:00"), m("18:00"))))
    }

    @Test
    fun `sd is zero for identical times and grows with spread`() {
        assertEquals(0.0, CircularTime.sd(listOf(m("23:00"), m("23:00"), m("23:00")))!!, 0.01)
        val tight = CircularTime.sd(listOf(m("22:50"), m("23:00"), m("23:10")))!!
        val wrapped = CircularTime.sd(listOf(m("23:50"), m("00:00"), m("00:10")))!!
        assertEquals(tight, wrapped, 0.01) // same spread, across midnight or not
        assertEquals(8.2, tight, 0.2) // population SD of ±10 min ≈ 8.16
        assertNull(CircularTime.sd(emptyList()))
    }

    @Test
    fun `diff and format`() {
        assertEquals(90, CircularTime.diff(m("00:30"), m("23:00")))
        assertEquals(-90, CircularTime.diff(m("23:00"), m("00:30")))
        assertEquals("03:05", CircularTime.format(185))
        assertEquals("23:00", CircularTime.format(-60))
    }
}

class SleepMetricsTest {
    private val weekdays = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    )

    private fun mains(sessions: List<SleepSession>, tags: Map<Long, Set<Long>> = emptyMap(), dayOff: Long? = null) =
        SleepMetrics.mainSleeps(sessions, SessionClassifier.classify(sessions), tags, weekdays, dayOff)

    @Test
    fun `daily totals split at midnight`() {
        val totals = SleepMetrics.dailyTotals(
            listOf(
                session(1, "2026-11-16T23:00", "2026-11-17T07:00"),
                session(2, "2026-11-17T14:00", "2026-11-17T14:30"),
            ),
        )
        assertEquals(Duration.ofHours(1), totals[LocalDate.parse("2026-11-16")])
        assertEquals(Duration.ofMinutes(7 * 60 + 30), totals[LocalDate.parse("2026-11-17")])
    }

    @Test
    fun `daily totals ignore active sessions and handle DST`() {
        val totals = SleepMetrics.dailyTotals(
            listOf(
                session(1, "2026-10-25T00:00", "2026-10-25T07:00"), // Kyiv falls back: 8 real hours
                session(2, "2026-11-20T23:00", null),
            ),
        )
        assertEquals(Duration.ofHours(8), totals[LocalDate.parse("2026-10-25")])
        assertEquals(1, totals.size)
    }

    @Test
    fun `main sleep values and free days`() {
        val list = mains(
            listOf(
                session(1, "2026-11-19T23:00", "2026-11-20T07:00"), // ends Fri → work day
                session(2, "2026-11-20T23:30", "2026-11-21T09:30"), // Fri night → Sat → free
            ),
        )
        val fri = list.first { it.session.id == 1L }
        assertEquals(23 * 60, fri.bedtime)
        assertEquals(7 * 60, fri.wake)
        assertEquals(3 * 60, fri.midpoint)
        assertEquals(false, fri.isFreeDay)
        assertEquals(true, list.first { it.session.id == 2L }.isFreeDay)
    }

    @Test
    fun `day off tag makes a work day free`() {
        val list = mains(listOf(session(1, "2026-11-17T23:00", "2026-11-18T07:00")), mapOf(1L to setOf(99L)), dayOff = 99L)
        assertTrue(list.single().isFreeDay)
    }

    @Test
    fun `consistency is 100 for identical nights and needs three`() {
        val same = (16..18).map { d -> session(d.toLong(), "2026-11-${d}T23:00", "2026-11-${d + 1}T07:00") }
        assertEquals(100, SleepMetrics.consistencyScore(mains(same)))
        assertNull(SleepMetrics.consistencyScore(mains(same.take(2))))
    }

    @Test
    fun `consistency drops with irregular nights`() {
        val messy = listOf(
            session(1, "2026-11-16T21:00", "2026-11-17T05:00"), // ends 17th
            session(2, "2026-11-18T01:00", "2026-11-18T09:30"), // ends 18th
            session(3, "2026-11-18T23:00", "2026-11-19T11:00"), // ends 19th
        )
        val score = SleepMetrics.consistencyScore(mains(messy))!!
        assertTrue("score $score should be low", score < 30)
    }

    @Test
    fun `social jetlag from the spec example`() {
        // Weekdays 00:00–07:00 (midpoint 03:30), weekend 02:00–11:00 (midpoint 06:30) → 3h.
        val sessions = listOf(
            session(1, "2026-11-17T00:00", "2026-11-17T07:00"), // Tue
            session(2, "2026-11-18T00:00", "2026-11-18T07:00"), // Wed
            session(3, "2026-11-21T02:00", "2026-11-21T11:00"), // Sat
            session(4, "2026-11-22T02:00", "2026-11-22T11:00"), // Sun
        )
        assertEquals(180, SleepMetrics.socialJetlagMinutes(mains(sessions)))
        assertNull(SleepMetrics.socialJetlagMinutes(mains(sessions.take(2))))
    }

    @Test
    fun `sleep debt skips unlogged days and never goes negative`() {
        val d = LocalDate.parse("2026-11-20")
        val target = Duration.ofHours(8)
        val totals = mapOf(
            d to Duration.ofHours(6), // -2h
            d.minusDays(1) to Duration.ofHours(7), // -1h
            d.minusDays(3) to Duration.ofHours(8), // 0
            d.minusDays(9) to Duration.ofHours(1), // outside window
        )
        assertEquals(Duration.ofHours(3), SleepMetrics.sleepDebt(totals, target, d))
        val surplus = mapOf(d to Duration.ofHours(10))
        assertEquals(Duration.ZERO, SleepMetrics.sleepDebt(surplus, target, d))
        assertEquals(Duration.ZERO, SleepMetrics.sleepDebt(emptyMap(), target, d))
    }

    @Test
    fun `actogram segments split across days`() {
        val s = session(1, "2026-11-16T23:00", "2026-11-17T07:00")
        val rows = SleepMetrics.daySegments(listOf(s), mapOf(1L to SessionType.MAIN), at("2026-11-20T12:00"))
        assertEquals(listOf(DaySegment(1, 23 * 60, 24 * 60, SessionType.MAIN)), rows[LocalDate.parse("2026-11-16")])
        assertEquals(listOf(DaySegment(1, 0, 7 * 60, SessionType.MAIN)), rows[LocalDate.parse("2026-11-17")])
    }

    @Test
    fun `active session is drawn until now`() {
        val s = session(1, "2026-11-20T23:00", null)
        val rows = SleepMetrics.daySegments(listOf(s), emptyMap(), at("2026-11-21T02:00"))
        assertEquals(2 * 60, rows[LocalDate.parse("2026-11-21")]!!.single().endMin)
    }
}

class PeriodsTest {
    private val d = LocalDate.parse("2026-11-19") // Thursday

    @Test
    fun ranges() {
        assertEquals(DateRange(LocalDate.parse("2026-11-16"), LocalDate.parse("2026-11-22")), Periods.range(Period.WEEK, d))
        assertEquals(DateRange(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30")), Periods.range(Period.MONTH, d))
        assertEquals(DateRange(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")), Periods.range(Period.YEAR, d))
        assertEquals(LocalDate.parse("2026-10-19"), Periods.shift(Period.MONTH, d, -1))
    }

    @Test
    fun `week trend has a point per day, year per week`() {
        val week = Periods.range(Period.WEEK, d)
        assertEquals(7, PeriodStats.trend(Period.WEEK, week, emptyList(), emptyMap()).size)
        val year = PeriodStats.trend(Period.YEAR, Periods.range(Period.YEAR, d), emptyList(), emptyMap())
        assertEquals(LocalDate.parse("2026-01-01"), year.first().start) // clipped to the year
        assertTrue(year.size in 52..54)
        assertTrue(PeriodStats.trend(Period.DAY, DateRange(d, d), emptyList(), emptyMap()).isEmpty())
    }

    @Test
    fun `summary averages`() {
        val sessions = listOf(
            session(1, "2026-11-16T23:00", "2026-11-17T07:00"),
            session(2, "2026-11-17T23:30", "2026-11-18T06:30"),
        )
        val mains = SleepMetrics.mainSleeps(sessions, SessionClassifier.classify(sessions), emptyMap(), DayOfWeek.entries.take(5).toSet(), null)
        val totals = SleepMetrics.dailyTotals(sessions)
        val s = PeriodStats.summary(Periods.range(Period.WEEK, d), mains, totals, Duration.ofHours(8), d)
        assertEquals(3, s.loggedDays) // 16th (1h), 17th (7h + 0.5h), 18th (6.5h)
        assertEquals(23 * 60 + 15, s.avgBedtime)
        assertNull(s.consistency)
    }
}
