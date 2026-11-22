package com.sighted.sleep.domain.sample

import com.sighted.sleep.domain.KYIV
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

class SampleDataTest {
    private val last = LocalDate.parse("2026-11-21")
    private val data = SampleData.generate(last, 90, KYIV)

    @Test
    fun `one night per day plus some naps`() {
        val nights = data.filter { Duration.between(it.start, it.end) >= Duration.ofHours(4) }
        assertEquals(90, nights.size)
        assertTrue(data.size in 90..120)
    }

    @Test
    fun `sessions never overlap and stay in range`() {
        val sorted = data.sortedBy { it.start }
        sorted.zipWithNext().forEach { (a, b) -> assertTrue("overlap at ${a.start}", !a.end.isAfter(b.start)) }
        val limit = last.plusDays(1).atStartOfDay(KYIV).toInstant()
        assertTrue(sorted.all { !it.end.isAfter(limit) && it.end.isAfter(it.start) })
    }

    @Test
    fun `quality is 1 to 10 when present`() {
        assertTrue(data.mapNotNull { it.quality }.all { it in 1..10 })
    }

    @Test
    fun `deterministic for a seed`() {
        assertEquals(data, SampleData.generate(last, 90, KYIV))
    }
}
