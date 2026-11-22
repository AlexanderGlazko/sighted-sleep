package com.sighted.sleep.domain

import com.sighted.sleep.domain.model.SleepSession
import java.time.Duration
import java.time.Instant

sealed interface ValidationError {
    data object EndBeforeStart : ValidationError
    data object StartInFuture : ValidationError
    data object EndInFuture : ValidationError
    data object AnotherSessionActive : ValidationError
    data class Overlaps(val other: SleepSession) : ValidationError
}

/** Session rules from spec §3.6: one active session at most, no overlaps, nothing in the future. */
object SessionValidator {
    /** Small allowance so "now" picked in a time picker isn't rejected as future. */
    private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(1)

    /**
     * @param id id of the session being edited, or null for a new one (it's excluded from overlap checks).
     * @param end null means the session is (or stays) active.
     */
    fun validate(
        id: Long?,
        start: Instant,
        end: Instant?,
        existing: List<SleepSession>,
        now: Instant,
    ): ValidationError? {
        val limit = now.plus(FUTURE_TOLERANCE)
        if (start.isAfter(limit)) return ValidationError.StartInFuture
        if (end != null) {
            if (!end.isAfter(start)) return ValidationError.EndBeforeStart
            if (end.isAfter(limit)) return ValidationError.EndInFuture
        }

        val others = existing.filter { it.id != id }
        if (end == null && others.any { it.isActive }) return ValidationError.AnotherSessionActive

        val candidateEnd = end ?: now
        others.forEach { other ->
            val otherEnd = other.end ?: now
            // Touching end-to-start is fine; any real intersection is not.
            if (start.isBefore(otherEnd) && other.start.isBefore(candidateEnd)) {
                return ValidationError.Overlaps(other)
            }
        }
        return null
    }
}
