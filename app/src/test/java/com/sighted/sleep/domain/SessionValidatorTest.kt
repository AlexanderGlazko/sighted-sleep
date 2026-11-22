package com.sighted.sleep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionValidatorTest {
    private val now = at("2026-11-21T12:00")
    private val lastNight = session(1, "2026-11-20T23:00", "2026-11-21T07:00")

    private fun validate(id: Long?, start: String, end: String?, existing: List<com.sighted.sleep.domain.model.SleepSession> = listOf(lastNight)) =
        SessionValidator.validate(id, at(start), end?.let { at(it) }, existing, now)

    @Test
    fun `valid manual nap passes`() {
        assertNull(validate(null, "2026-11-21T09:00", "2026-11-21T10:00"))
    }

    @Test
    fun `end must be after start`() {
        assertEquals(ValidationError.EndBeforeStart, validate(null, "2026-11-21T10:00", "2026-11-21T09:00"))
        assertEquals(ValidationError.EndBeforeStart, validate(null, "2026-11-21T10:00", "2026-11-21T10:00"))
    }

    @Test
    fun `future times are rejected`() {
        assertEquals(ValidationError.StartInFuture, validate(null, "2026-11-21T13:00", null))
        assertEquals(ValidationError.EndInFuture, validate(null, "2026-11-21T11:00", "2026-11-21T13:00"))
    }

    @Test
    fun `now within one minute tolerance is accepted`() {
        assertNull(validate(null, "2026-11-21T11:00", "2026-11-21T12:00"))
    }

    @Test
    fun `overlapping session is rejected`() {
        val error = validate(null, "2026-11-21T06:00", "2026-11-21T08:00")
        assertTrue(error is ValidationError.Overlaps)
        assertEquals(1L, (error as ValidationError.Overlaps).other.id)
    }

    @Test
    fun `session fully inside another is rejected`() {
        assertTrue(validate(null, "2026-11-21T01:00", "2026-11-21T02:00") is ValidationError.Overlaps)
    }

    @Test
    fun `touching sessions are allowed`() {
        assertNull(validate(null, "2026-11-21T07:00", "2026-11-21T08:00"))
        assertNull(validate(null, "2026-11-20T21:00", "2026-11-20T23:00"))
    }

    @Test
    fun `editing a session does not overlap with itself`() {
        assertNull(validate(1, "2026-11-20T22:30", "2026-11-21T07:30"))
    }

    @Test
    fun `only one active session allowed`() {
        val active = session(2, "2026-11-21T11:00", null)
        assertEquals(
            ValidationError.AnotherSessionActive,
            validate(null, "2026-11-21T11:30", null, listOf(lastNight, active)),
        )
    }

    @Test
    fun `active session counts as running until now for overlaps`() {
        val active = session(2, "2026-11-21T10:00", null)
        assertTrue(validate(null, "2026-11-21T10:30", "2026-11-21T11:00", listOf(active)) is ValidationError.Overlaps)
    }

    @Test
    fun `empty history accepts anything valid`() {
        assertNull(validate(null, "2026-11-20T23:00", "2026-11-21T07:00", emptyList()))
    }
}
