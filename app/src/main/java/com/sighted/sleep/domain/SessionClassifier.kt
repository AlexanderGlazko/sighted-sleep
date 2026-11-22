package com.sighted.sleep.domain

import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession

/**
 * Main sleep vs nap (spec §3.2).
 *
 * Sessions are grouped by the calendar day they ended. Per day:
 * - manual overrides always win;
 * - if no session that day is manually MAIN, the longest automatic session becomes MAIN
 *   (ties go to the earlier start); every other automatic session is a NAP.
 *
 * Active sessions are not classified.
 */
object SessionClassifier {
    fun classify(sessions: List<SleepSession>): Map<Long, SessionType> {
        val result = mutableMapOf<Long, SessionType>()
        sessions
            .filter { !it.isActive }
            .groupBy { it.endDate!! }
            .values
            .forEach { day ->
                day.forEach { s -> s.typeOverride?.let { result[s.id] = it } }
                val auto = day.filter { it.typeOverride == null }
                val hasManualMain = day.any { it.typeOverride == SessionType.MAIN }
                val main = if (hasManualMain) null else auto.maxWithOrNull(
                    compareBy<SleepSession> { it.duration(it.end!!) }.thenByDescending { it.start },
                )
                auto.forEach { s -> result[s.id] = if (s.id == main?.id) SessionType.MAIN else SessionType.NAP }
            }
        return result
    }
}
