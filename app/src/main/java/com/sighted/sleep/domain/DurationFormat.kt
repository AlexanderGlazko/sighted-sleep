package com.sighted.sleep.domain

import java.time.Duration

/** "7h 35m", "45m", "0m". Negative durations are treated as zero. */
fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours == 0L) "${minutes}m" else "${hours}h ${minutes}m"
}

/** Live timer: "0:00:12", "6:12:05". Negative durations are treated as zero. */
fun formatElapsed(duration: Duration): String {
    val total = duration.seconds.coerceAtLeast(0)
    return "%d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}

/** "on time" within ±5 min, otherwise "47m late" / "1h 10m early". */
fun formatDeviation(minutes: Int): String = when {
    kotlin.math.abs(minutes) <= 5 -> "on time"
    minutes > 0 -> "${formatDuration(Duration.ofMinutes(minutes.toLong()))} late"
    else -> "${formatDuration(Duration.ofMinutes(-minutes.toLong()))} early"
}
