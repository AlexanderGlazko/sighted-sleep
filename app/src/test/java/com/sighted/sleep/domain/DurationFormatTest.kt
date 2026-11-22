package com.sighted.sleep.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class DurationFormatTest {
    @Test
    fun formats() {
        assertEquals("0m", formatDuration(Duration.ZERO))
        assertEquals("45m", formatDuration(Duration.ofMinutes(45)))
        assertEquals("1h 0m", formatDuration(Duration.ofMinutes(60)))
        assertEquals("7h 35m", formatDuration(Duration.ofMinutes(455)))
        assertEquals("0m", formatDuration(Duration.ofMinutes(-5)))
        assertEquals("25h 0m", formatDuration(Duration.ofHours(25)))
    }

    @Test
    fun elapsed() {
        assertEquals("0:00:00", formatElapsed(Duration.ZERO))
        assertEquals("0:00:12", formatElapsed(Duration.ofSeconds(12)))
        assertEquals("6:12:05", formatElapsed(Duration.ofSeconds(6 * 3600 + 12 * 60 + 5)))
        assertEquals("0:00:00", formatElapsed(Duration.ofSeconds(-3)))
    }

    @Test
    fun deviation() {
        assertEquals("on time", formatDeviation(0))
        assertEquals("on time", formatDeviation(-5))
        assertEquals("47m late", formatDeviation(47))
        assertEquals("1h 10m early", formatDeviation(-70))
    }
}
