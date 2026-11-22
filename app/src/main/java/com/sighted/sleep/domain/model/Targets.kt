package com.sighted.sleep.domain.model

import java.time.Duration
import java.time.LocalTime

/** One target schedule for every day (spec §3.5). */
data class Targets(
    val bedtime: LocalTime = LocalTime.of(23, 0),
    val wake: LocalTime = LocalTime.of(7, 0),
) {
    /** Bedtime → wake, across midnight when needed. Equal times mean a full 24h (degenerate, but non-zero). */
    val duration: Duration
        get() {
            val minutes = Duration.between(bedtime, wake).toMinutes().let { if (it <= 0) it + 24 * 60 else it }
            return Duration.ofMinutes(minutes)
        }
}
