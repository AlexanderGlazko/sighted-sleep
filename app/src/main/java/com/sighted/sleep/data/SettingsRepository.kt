package com.sighted.sleep.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sighted.sleep.domain.model.Targets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** User settings stored with DataStore (spec §7). Times are stored as minutes after midnight. */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val BEDTIME = intPreferencesKey("target_bedtime_min")
        val WAKE = intPreferencesKey("target_wake_min")
    }

    val targets: Flow<Targets> = context.settingsStore.data.map { prefs ->
        val defaults = Targets()
        Targets(
            bedtime = prefs[Keys.BEDTIME]?.toLocalTime() ?: defaults.bedtime,
            wake = prefs[Keys.WAKE]?.toLocalTime() ?: defaults.wake,
        )
    }

    suspend fun currentTargets(): Targets = targets.first()

    suspend fun setBedtime(time: LocalTime) {
        context.settingsStore.edit { it[Keys.BEDTIME] = time.toMinutes() }
    }

    suspend fun setWake(time: LocalTime) {
        context.settingsStore.edit { it[Keys.WAKE] = time.toMinutes() }
    }
}

private fun Int.toLocalTime(): LocalTime = LocalTime.of((this / 60) % 24, this % 60)
private fun LocalTime.toMinutes(): Int = hour * 60 + minute
