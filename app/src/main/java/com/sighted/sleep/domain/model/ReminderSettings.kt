package com.sighted.sleep.domain.model

/** Optional reminders (spec F6), all off by default. */
data class ReminderSettings(
    val bedtimeEnabled: Boolean = false,
    /** Minutes before target bedtime. */
    val bedtimeOffsetMin: Int = 30,
    val morningEnabled: Boolean = false,
    /** Minutes after waking, if the sleep is still unrated. */
    val morningDelayMin: Int = 60,
    /** "Still sleeping?" notification after 14h (spec §3.6). */
    val stillSleepingEnabled: Boolean = false,
) {
    val anyEnabled: Boolean get() = bedtimeEnabled || morningEnabled || stillSleepingEnabled
}
