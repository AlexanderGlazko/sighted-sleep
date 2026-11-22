package com.sighted.sleep.domain

import com.sighted.sleep.domain.model.SessionType.MAIN
import com.sighted.sleep.domain.model.SessionType.NAP
import com.sighted.sleep.domain.model.Targets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class TargetMathTest {
    private fun t(s: String) = LocalTime.parse(s)

    @Test
    fun `deviation is signed and wraps around midnight`() {
        assertEquals(0, TargetMath.deviationMinutes(t("23:00"), t("23:00")))
        assertEquals(47, TargetMath.deviationMinutes(t("23:47"), t("23:00")))
        assertEquals(-30, TargetMath.deviationMinutes(t("22:30"), t("23:00")))
        assertEquals(90, TargetMath.deviationMinutes(t("00:30"), t("23:00")))
        assertEquals(-60, TargetMath.deviationMinutes(t("23:30"), t("00:30")))
        assertEquals(720, TargetMath.deviationMinutes(t("11:00"), t("23:00")))
    }

    @Test
    fun `target duration crosses midnight`() {
        assertEquals(Duration.ofHours(8), Targets(t("23:00"), t("07:00")).duration)
        assertEquals(Duration.ofMinutes(450), Targets(t("00:30"), t("08:00")).duration)
        assertEquals(Duration.ofHours(8), Targets(t("14:00"), t("22:00")).duration)
        assertEquals(Duration.ofHours(24), Targets(t("07:00"), t("07:00")).duration)
    }

    @Test
    fun `default manual range is the last finished target night`() {
        val targets = Targets(t("23:00"), t("07:00"))
        // Morning after target wake → last night, ending this morning.
        val (s1, e1) = TargetMath.defaultManualRange(LocalDateTime.parse("2026-11-22T09:15"), targets)
        assertEquals(LocalDateTime.parse("2026-11-21T23:00"), s1)
        assertEquals(LocalDateTime.parse("2026-11-22T07:00"), e1)
        // Small hours before target wake → the night that ended yesterday.
        val (s2, e2) = TargetMath.defaultManualRange(LocalDateTime.parse("2026-11-22T02:40"), targets)
        assertEquals(LocalDateTime.parse("2026-11-20T23:00"), s2)
        assertEquals(LocalDateTime.parse("2026-11-21T07:00"), e2)
    }

    @Test
    fun `latest sleep prefers last main and lists later naps`() {
        val night = session(1, "2026-11-20T23:00", "2026-11-21T07:00")
        val nap = session(2, "2026-11-21T14:00", "2026-11-21T14:30")
        val earlierNap = session(3, "2026-11-20T15:00", "2026-11-20T15:20")
        val types = mapOf(1L to MAIN, 2L to NAP, 3L to MAIN)
        val result = latestSleep(listOf(nap, night, earlierNap), types)
        assertEquals(1L, result.main?.id)
        assertEquals(listOf(2L), result.napsAfter.map { it.id })
    }

    @Test
    fun `latest sleep with no sessions is empty`() {
        val result = latestSleep(emptyList(), emptyMap())
        assertNull(result.main)
        assertTrue(result.napsAfter.isEmpty())
    }
}
