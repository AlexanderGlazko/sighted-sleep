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
}
