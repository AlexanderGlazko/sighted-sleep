package com.sighted.sleep

import android.app.Application
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.db.SightedSleepDatabase

/** Holds app-wide singletons (simple manual dependency injection). */
class SightedSleepApp : Application() {
    val database: SightedSleepDatabase by lazy { SightedSleepDatabase.create(this) }
    val sleepRepository: SleepRepository by lazy { SleepRepository(database.sessionDao()) }
}
