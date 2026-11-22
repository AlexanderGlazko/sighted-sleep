package com.sighted.sleep.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class SessionType { MAIN, NAP }

/** Domain view of a sleep session. Pure Kotlin, no Android types. */
data class SleepSession(
    val id: Long,
    val start: Instant,
    /** null = still sleeping. */
    val end: Instant?,
    val zone: ZoneId,
    /** null = classified automatically (spec §3.2). */
    val typeOverride: SessionType? = null,
    val quality: Int? = null,
    val comment: String? = null,
) {
    val isActive: Boolean get() = end == null

    /** Duration so far for an active session, full duration otherwise. */
    fun duration(now: Instant): Duration = Duration.between(start, end ?: now)

    /** Calendar day the session is attributed to: the local date it ended (spec §3.3). */
    val endDate: LocalDate? get() = end?.atZone(zone)?.toLocalDate()
}

object SessionRules {
    /** Shorter sessions prompt "Discard this session?" when stopped (spec §3.6). */
    val MIN_DURATION: Duration = Duration.ofMinutes(5)

    /** Active sessions older than this show "Still sleeping?" (spec §3.6). */
    val STILL_SLEEPING_AFTER: Duration = Duration.ofHours(14)
}
