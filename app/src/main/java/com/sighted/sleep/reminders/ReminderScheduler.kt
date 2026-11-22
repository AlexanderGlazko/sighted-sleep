package com.sighted.sleep.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sighted.sleep.SightedSleepApp
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.domain.reminders.ReminderTimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Keeps the three reminder alarms (spec F6.1–F6.3, §3.6) in sync with settings and data.
 * Called after any data or settings change, on app start, after reboot and on time/zone changes.
 * Each alarm re-checks the situation when it fires, so a stale alarm never shows a wrong reminder.
 */
class ReminderScheduler(
    private val context: Context,
    private val settings: SettingsRepository,
    private val sleep: SleepRepository,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    suspend fun rescheduleAll() {
        val r = settings.currentReminders()
        val now = Instant.now()
        val sessions = sleep.sessions.first()
        val active = sessions.firstOrNull { it.isActive }

        // F6.1 bedtime: next occurrence, skipped while already asleep (re-planned when you wake).
        if (r.bedtimeEnabled && active == null) {
            val at = ReminderTimes.nextBedtimeReminder(ZonedDateTime.now(ZoneId.systemDefault()), settings.currentTargets().bedtime, r.bedtimeOffsetMin)
            set(ReminderReceiver.BEDTIME, at.toInstant(), null)
        } else {
            cancel(ReminderReceiver.BEDTIME)
        }

        // §3.6 still sleeping: 14h after the running sleep started (only if that's still ahead).
        val stillAt = active?.let { ReminderTimes.stillSleepingAt(it.start) }
        if (r.stillSleepingEnabled && active != null && stillAt!!.isAfter(now)) {
            set(ReminderReceiver.STILL_SLEEPING, stillAt, active.id)
        } else {
            cancel(ReminderReceiver.STILL_SLEEPING)
            if (active == null) Notifier.cancel(context, Notifier.ID_STILL_SLEEPING)
        }

        // F6.2 morning: after the latest finished sleep, if it's unrated.
        val last = sessions.filter { !it.isActive }.maxByOrNull { it.end!! }
        val morningAt = last?.let { ReminderTimes.morningReminderAt(it.end!!, r.morningDelayMin) }
        if (r.morningEnabled && last != null && last.quality == null && morningAt!!.isAfter(now)) {
            set(ReminderReceiver.MORNING, morningAt, last.id)
        } else {
            cancel(ReminderReceiver.MORNING)
            if (last?.quality != null) Notifier.cancel(context, Notifier.ID_MORNING)
        }
    }

    private fun pending(action: String, sessionId: Long?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        sessionId?.let { intent.putExtra(AppRequest.EXTRA_SESSION_ID, it) }
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun set(action: String, at: Instant, sessionId: Long?) {
        val pi = pending(action, sessionId)
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi)
        } else {
            // Without exact-alarm access the reminder may arrive a few minutes late, but it still arrives.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi)
        }
    }

    private fun cancel(action: String) = alarms.cancel(pending(action, null))
}

/** Fires reminders. Re-checks state first so outdated alarms stay silent. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SightedSleepApp
        val sessionId = intent.getLongExtra(AppRequest.EXTRA_SESSION_ID, -1L)
        val done = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val r = app.settingsRepository.currentReminders()
                when (intent.action) {
                    BEDTIME -> if (r.bedtimeEnabled && app.sleepRepository.getActive() == null) {
                        val bed = app.settingsRepository.currentTargets().bedtime
                        Notifier.post(
                            context, Notifier.ID_BEDTIME,
                            "Time to wind down",
                            "Target bedtime $bed is in ${r.bedtimeOffsetMin} min.",
                            AppRequest.intent(context, Intent.ACTION_MAIN),
                        )
                    }
                    MORNING -> {
                        val s = app.sleepRepository.getSession(sessionId)
                        if (r.morningEnabled && s != null && !s.isActive && s.quality == null) {
                            Notifier.post(
                                context, Notifier.ID_MORNING,
                                "How did you sleep?",
                                "Rate last night and add tags — it takes 10 seconds.",
                                AppRequest.intent(context, AppRequest.ACTION_CHECK_IN, s.id),
                            )
                        }
                    }
                    STILL_SLEEPING -> {
                        val active = app.sleepRepository.getActive()
                        if (r.stillSleepingEnabled && active != null && active.id == sessionId) {
                            Notifier.post(
                                context, Notifier.ID_STILL_SLEEPING,
                                "Still sleeping?",
                                "Your sleep has been running for 14 hours. Did you forget to tap Wake up?",
                                AppRequest.intent(context, AppRequest.ACTION_EDIT, active.id),
                                actions = listOf(
                                    "Wake up now" to AppRequest.intent(context, AppRequest.ACTION_WAKE),
                                    "Edit end time" to AppRequest.intent(context, AppRequest.ACTION_EDIT, active.id),
                                ),
                            )
                        }
                    }
                }
                app.reminderScheduler.rescheduleAll() // plan the next one (e.g. tomorrow's bedtime)
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        const val BEDTIME = "com.sighted.sleep.reminder.BEDTIME"
        const val MORNING = "com.sighted.sleep.reminder.MORNING"
        const val STILL_SLEEPING = "com.sighted.sleep.reminder.STILL_SLEEPING"
    }
}

/** Alarms are cleared on reboot and can drift on time/zone changes: re-plan them (spec F6.3). */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SightedSleepApp
        val done = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.reminderScheduler.rescheduleAll()
            } finally {
                done.finish()
            }
        }
    }
}
