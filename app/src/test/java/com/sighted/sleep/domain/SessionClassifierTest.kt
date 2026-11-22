package com.sighted.sleep.domain

import com.sighted.sleep.domain.model.SessionType.MAIN
import com.sighted.sleep.domain.model.SessionType.NAP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionClassifierTest {

    @Test
    fun `single session is main`() {
        val result = SessionClassifier.classify(listOf(session(1, "2026-11-20T23:00", "2026-11-21T07:00")))
        assertEquals(mapOf(1L to MAIN), result)
    }

    @Test
    fun `longest session of the day is main, others are naps`() {
        val result = SessionClassifier.classify(
            listOf(
                session(1, "2026-11-20T23:30", "2026-11-21T06:30"), // 7h, ends 21st
                session(2, "2026-11-21T14:00", "2026-11-21T14:40"), // nap, ends 21st
            ),
        )
        assertEquals(mapOf(1L to MAIN, 2L to NAP), result)
    }

    @Test
    fun `sessions are grouped by the day they end, crossing midnight`() {
        val result = SessionClassifier.classify(
            listOf(
                session(1, "2026-11-20T15:00", "2026-11-20T15:30"), // only session ending on the 20th
                session(2, "2026-11-20T23:00", "2026-11-21T07:00"), // ends on the 21st
            ),
        )
        assertEquals(mapOf(1L to MAIN, 2L to MAIN), result)
    }

    @Test
    fun `tie goes to the earlier session`() {
        val result = SessionClassifier.classify(
            listOf(
                session(1, "2026-11-21T01:00", "2026-11-21T04:00"),
                session(2, "2026-11-21T13:00", "2026-11-21T16:00"),
            ),
        )
        assertEquals(mapOf(1L to MAIN, 2L to NAP), result)
    }

    @Test
    fun `manual main override makes all automatic sessions naps`() {
        val result = SessionClassifier.classify(
            listOf(
                session(1, "2026-11-20T23:00", "2026-11-21T07:00"),
                session(2, "2026-11-21T13:00", "2026-11-21T14:00", override = MAIN),
            ),
        )
        assertEquals(mapOf(1L to NAP, 2L to MAIN), result)
    }

    @Test
    fun `manual nap override passes main to the next longest`() {
        val result = SessionClassifier.classify(
            listOf(
                session(1, "2026-11-20T23:00", "2026-11-21T07:00", override = NAP),
                session(2, "2026-11-21T13:00", "2026-11-21T14:00"),
            ),
        )
        assertEquals(mapOf(1L to NAP, 2L to MAIN), result)
    }

    @Test
    fun `active session is not classified`() {
        val result = SessionClassifier.classify(listOf(session(1, "2026-11-21T23:00", null)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty input gives empty result`() {
        assertTrue(SessionClassifier.classify(emptyList()).isEmpty())
    }

    @Test
    fun `DST night is measured in real elapsed time`() {
        // Kyiv falls back on 2026-10-25 at 04:00 → 03:00, so 00:00–07:00 is 8 real hours.
        val dstNight = session(1, "2026-10-25T00:00", "2026-10-25T07:00")
        val nap = session(2, "2026-10-25T09:00", "2026-10-25T16:30") // 7.5h
        val result = SessionClassifier.classify(listOf(dstNight, nap))
        assertEquals(mapOf(1L to MAIN, 2L to NAP), result)
    }
}
