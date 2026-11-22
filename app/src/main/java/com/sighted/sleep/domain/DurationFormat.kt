package com.sighted.sleep.domain

import java.time.Duration

/** "7h 35m", "45m", "0m". Negative durations are treated as zero. */
fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours == 0L) "${minutes}m" else "${hours}h ${minutes}m"
}
