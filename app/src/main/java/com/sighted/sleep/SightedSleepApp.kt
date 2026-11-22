package com.sighted.sleep

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.data.db.SightedSleepDatabase
import com.sighted.sleep.widget.SleepWidget

/** Holds app-wide singletons (simple manual dependency injection). */
class SightedSleepApp : Application() {
    val database: SightedSleepDatabase by lazy { SightedSleepDatabase.create(this) }

    val sleepRepository: SleepRepository by lazy {
        SleepRepository(
            dao = database.sessionDao(),
            checkInDao = database.checkInDao(),
            onSessionsChanged = { runCatching { SleepWidget().updateAll(this) } },
        )
    }

    val tagRepository: TagRepository by lazy { TagRepository(database.tagDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
