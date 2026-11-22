package com.sighted.sleep.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sighted.sleep.domain.backup.BackupReminders
import com.sighted.sleep.domain.backup.BackupSettings
import com.sighted.sleep.domain.model.ReminderSettings
import com.sighted.sleep.domain.model.Targets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Mon–Fri by default (spec §3.4). */
val DEFAULT_WORK_DAYS: Set<DayOfWeek> = DayOfWeek.entries.take(5).toSet()

/** User settings stored with DataStore (spec §7). Times are stored as minutes after midnight. */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val BEDTIME = intPreferencesKey("target_bedtime_min")
        val WAKE = intPreferencesKey("target_wake_min")
        val WORK_DAYS = stringSetPreferencesKey("work_days")
        val R_BED_ON = booleanPreferencesKey("reminder_bedtime_on")
        val R_BED_OFFSET = intPreferencesKey("reminder_bedtime_offset_min")
        val R_MORNING_ON = booleanPreferencesKey("reminder_morning_on")
        val R_MORNING_DELAY = intPreferencesKey("reminder_morning_delay_min")
        val R_STILL_ON = booleanPreferencesKey("reminder_still_sleeping_on")
    }

    val targets: Flow<Targets> = context.settingsStore.data.map { prefs ->
        val defaults = Targets()
        Targets(
            bedtime = prefs[Keys.BEDTIME]?.toLocalTime() ?: defaults.bedtime,
            wake = prefs[Keys.WAKE]?.toLocalTime() ?: defaults.wake,
        )
    }

    val workDays: Flow<Set<DayOfWeek>> = context.settingsStore.data.map { prefs ->
        prefs[Keys.WORK_DAYS]?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }?.toSet()
            ?: DEFAULT_WORK_DAYS
    }

    val reminders: Flow<ReminderSettings> = context.settingsStore.data.map { p ->
        val d = ReminderSettings()
        ReminderSettings(
            bedtimeEnabled = p[Keys.R_BED_ON] ?: d.bedtimeEnabled,
            bedtimeOffsetMin = p[Keys.R_BED_OFFSET] ?: d.bedtimeOffsetMin,
            morningEnabled = p[Keys.R_MORNING_ON] ?: d.morningEnabled,
            morningDelayMin = p[Keys.R_MORNING_DELAY] ?: d.morningDelayMin,
            stillSleepingEnabled = p[Keys.R_STILL_ON] ?: d.stillSleepingEnabled,
        )
    }

    suspend fun currentTargets(): Targets = targets.first()
    suspend fun currentReminders(): ReminderSettings = reminders.first()

    suspend fun setBedtime(time: LocalTime) {
        context.settingsStore.edit { it[Keys.BEDTIME] = time.toMinutes() }
    }

    suspend fun setWake(time: LocalTime) {
        context.settingsStore.edit { it[Keys.WAKE] = time.toMinutes() }
    }

    suspend fun setWorkDays(days: Set<DayOfWeek>) {
        context.settingsStore.edit { it[Keys.WORK_DAYS] = days.map { d -> d.name }.toSet() }
    }

    suspend fun setReminders(r: ReminderSettings) {
        context.settingsStore.edit {
            it[Keys.R_BED_ON] = r.bedtimeEnabled
            it[Keys.R_BED_OFFSET] = r.bedtimeOffsetMin
            it[Keys.R_MORNING_ON] = r.morningEnabled
            it[Keys.R_MORNING_DELAY] = r.morningDelayMin
            it[Keys.R_STILL_ON] = r.stillSleepingEnabled
        }
    }

    /** Snapshot for backups (spec §7). */
    suspend fun toBackup(): BackupSettings {
        val t = currentTargets()
        val r = currentReminders()
        return BackupSettings(
            targetBedtimeMin = t.bedtime.toMinutes(),
            targetWakeMin = t.wake.toMinutes(),
            workDays = workDays.first().sortedBy { it.value }.map { it.name },
            reminders = BackupReminders(r.bedtimeEnabled, r.bedtimeOffsetMin, r.morningEnabled, r.morningDelayMin, r.stillSleepingEnabled),
        )
    }

    suspend fun restore(b: BackupSettings) {
        setBedtime(b.targetBedtimeMin.toLocalTime())
        setWake(b.targetWakeMin.toLocalTime())
        setWorkDays(b.workDays.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet())
        b.reminders.let {
            setReminders(ReminderSettings(it.bedtimeEnabled, it.bedtimeOffsetMin, it.morningEnabled, it.morningDelayMin, it.stillSleepingEnabled))
        }
    }
}

private fun Int.toLocalTime(): LocalTime = LocalTime.of(((this / 60) % 24 + 24) % 24, ((this % 60) + 60) % 60)
private fun LocalTime.toMinutes(): Int = hour * 60 + minute
