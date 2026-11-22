package com.sighted.sleep.domain.metrics

import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class InsightsTest {
    private val start = LocalDate.parse("2026-10-01")

    /** One main sleep per night: bedtime "HH:mm" on day i, 8h long unless [hours] given. */
    private fun night(i: Int, bed: String, quality: Int?, tags: Set<Long> = emptySet(), hours: Long = 8): Pair<SleepSession, Set<Long>> {
        val (h, m) = bed.split(":").map { it.toInt() }
        val day = start.plusDays(i.toLong()).let { if (h < 12) it.plusDays(1) else it }
        val s = day.atTime(h, m)
        val e = s.plusHours(hours)
        return session(i.toLong(), s.toString(), e.toString()).copy(quality = quality) to tags
    }

    private fun mains(nights: List<Pair<SleepSession, Set<Long>>>): List<MainSleep> {
        val sessions = nights.map { it.first }
        val tags = nights.associate { it.first.id to it.second }
        return SleepMetrics.mainSleeps(sessions, SessionClassifier.classify(sessions), tags, DayOfWeek.entries.toSet(), null)
    }

    @Test
    fun `buckets are 30 minutes and ordered through the night`() {
        val m = mains(
            listOf(
                night(0, "00:10", 6), night(1, "00:20", 8), // 00:00 bucket
                night(2, "23:40", 5), // 23:30 bucket
                night(3, "22:05", 9), // 22:00 bucket
            ),
        )
        val buckets = InsightMetrics.qualityByBedtime(m)
        assertEquals(listOf(22 * 60, 23 * 60 + 30, 0), buckets.map { it.startMin })
        assertEquals(7.0, buckets.last().avgQuality, 0.001)
        assertEquals(2, buckets.last().count)
        assertFalse(buckets.last().enoughData)
    }

    @Test
    fun `unrated sleeps are ignored and best needs three samples`() {
        val m = mains(
            listOf(
                night(0, "23:00", 9), night(1, "23:10", 8), night(2, "23:20", 7), // 23:00 → avg 8, n=3
                night(3, "00:30", 10), night(4, "00:40", 10), // better but n=2
                night(5, "23:15", null),
            ),
        )
        val buckets = InsightMetrics.qualityByBedtime(m)
        assertEquals(3, buckets.first { it.startMin == 23 * 60 }.count)
        assertEquals(23 * 60, InsightMetrics.bestBedtime(buckets)?.startMin)
    }

    @Test
    fun `no data gives no buckets and no best`() {
        assertTrue(InsightMetrics.qualityByBedtime(emptyList()).isEmpty())
        assertNull(InsightMetrics.bestBedtime(emptyList()))
    }

    @Test
    fun `tag impact compares with and without`() {
        val caffeine = 7L
        val m = mains(
            listOf(
                night(0, "00:30", 4, setOf(caffeine), hours = 7),
                night(1, "00:30", 5, setOf(caffeine), hours = 7),
                night(2, "00:30", 3, setOf(caffeine), hours = 7),
                night(3, "23:00", 7),
                night(4, "23:00", 8),
                night(5, "23:00", 9),
            ),
        )
        val impact = InsightMetrics.tagImpact(m, listOf(caffeine)).single()
        assertEquals(3, impact.withCount)
        assertEquals(-4.0, impact.qualityDelta!!, 0.001) // 4 vs 8
        assertEquals(-60, impact.durationDeltaMin) // 7h vs 8h
        // midpoints: 04:00 vs 03:00 → 1h later; also checks the circular mean across midnight
        assertEquals(60, impact.midpointShiftMin)
    }

    @Test
    fun `tag impact needs three on each side`() {
        val t = 1L
        val m = mains(listOf(night(0, "23:00", 5, setOf(t)), night(1, "23:00", 6), night(2, "23:00", 7), night(3, "23:00", 8)))
        val impact = InsightMetrics.tagImpact(m, listOf(t)).single()
        assertNull(impact.qualityDelta)
        assertNull(impact.durationDeltaMin)
        assertNull(impact.midpointShiftMin)
    }

    @Test
    fun `unused tags are skipped and results sorted by quality effect`() {
        val small = 1L
        val big = 2L
        val m = mains(
            (0 until 6).map { i ->
                val tags = buildSet { if (i < 3) add(big); if (i % 2 == 0) add(small) }
                night(i, "23:00", if (i < 3) 3 else 8, tags)
            },
        )
        val result = InsightMetrics.tagImpact(m, listOf(small, big, 99L))
        assertEquals(listOf(big, small), result.map { it.tagId })
    }
}
