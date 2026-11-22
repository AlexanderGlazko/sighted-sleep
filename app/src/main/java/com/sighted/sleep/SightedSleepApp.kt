package com.sighted.sleep

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.sighted.sleep.data.BackupRepository
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.data.db.SightedSleepDatabase
import com.sighted.sleep.reminders.Notifier
import com.sighted.sleep.reminders.ReminderScheduler
import com.sighted.sleep.widget.SleepWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Holds app-wide singletons (simple manual dependency injection). */
class SightedSleepApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: SightedSleepDatabase by lazy { SightedSleepDatabase.create(this) }

    val sleepRepository: SleepRepository by lazy {
        SleepRepository(
            dao = database.sessionDao(),
            checkInDao = database.checkInDao(),
            onSessionsChanged = {
                runCatching { SleepWidget().updateAll(this) }
                runCatching { reminderScheduler.rescheduleAll() }
            },
        )
    }

    val tagRepository: TagRepository by lazy { TagRepository(database.tagDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val backupRepository: BackupRepository by lazy { BackupRepository(database, settingsRepository, sleepRepository) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this, settingsRepository, sleepRepository) }

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        // Re-plan reminders whenever targets or reminder settings change (and once at start).
        appScope.launch {
            combine(settingsRepository.targets, settingsRepository.reminders) { t, r -> t to r }
                .distinctUntilChanged()
                .collect { runCatching { reminderScheduler.rescheduleAll() } }
        }
    }
}
