package com.sighted.sleep.ui.pages.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.domain.sample.SampleData
import com.sighted.sleep.data.BackupRepository
import com.sighted.sleep.domain.backup.BackupCodec
import com.sighted.sleep.domain.backup.BackupFile
import com.sighted.sleep.domain.backup.DecodeResult
import com.sighted.sleep.domain.model.ReminderSettings
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class SettingsUiState(
    val loaded: Boolean = false,
    val targets: Targets = Targets(),
    val workDays: Set<DayOfWeek> = emptySet(),
    val tags: List<Tag> = emptyList(),
    val reminders: ReminderSettings = ReminderSettings(),
)

/** A backup file that passed validation, waiting for the user to confirm "replace all". */
data class ImportPreview(val file: BackupFile, val exportedAt: String)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val tagRepository: TagRepository,
    private val sleepRepository: SleepRepository,
    private val backup: BackupRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(settings.targets, settings.workDays, tagRepository.tags, settings.reminders) { targets, workDays, tags, reminders ->
            SettingsUiState(loaded = true, targets = targets, workDays = workDays, tags = tags, reminders = reminders)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateReminders(change: (ReminderSettings) -> ReminderSettings) = viewModelScope.launch {
        settings.setReminders(change(settings.currentReminders()))
    }

    // --- Backup (spec F7.2, F7.3) ---

    suspend fun exportJson(): String = backup.exportJson()
    suspend fun exportCsv(): String = backup.exportCsv()

    /** Validates a picked file. Returns a preview to confirm, or an error message. */
    fun previewImport(text: String): Pair<ImportPreview?, String?> = when (val r = BackupCodec.decode(text)) {
        is DecodeResult.Invalid -> null to r.reason
        is DecodeResult.Ok -> {
            val at = runCatching {
                Instant.parse(r.file.exportedAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
            }.getOrDefault(r.file.exportedAt)
            ImportPreview(r.file, at) to null
        }
    }

    suspend fun import(preview: ImportPreview): String {
        backup.replaceAll(preview.file)
        return "Restored ${preview.file.sessions.size} sleeps and ${preview.file.tags.size} tags."
    }

    fun setBedtime(t: LocalTime) = viewModelScope.launch { settings.setBedtime(t) }
    fun setWake(t: LocalTime) = viewModelScope.launch { settings.setWake(t) }

    fun toggleWorkDay(day: DayOfWeek) = viewModelScope.launch {
        val current = state.value.workDays
        settings.setWorkDays(if (day in current) current - day else current + day)
    }

    /** Deletes an unused custom tag. Returns an error message, or null on success. */
    suspend fun deleteTag(tag: Tag): String? = tagRepository.delete(tag)

    /** Creates (existing == null) or updates a tag. Returns an error message, or null on success. */
    suspend fun saveTag(existing: Tag?, name: String, colorHex: String, archived: Boolean): String? =
        if (existing == null) {
            tagRepository.add(name, colorHex)
        } else {
            tagRepository.update(existing.copy(name = name, colorHex = colorHex, isArchived = archived))
        }

    /** Debug only: 90 days of sample history ending yesterday. Returns a message for the user. */
    suspend fun addSampleData(): String {
        val tags = tagRepository.tags.first().associate { it.name to it.id }
        val sessions = SampleData.generate(LocalDate.now().minusDays(1), 90, ZoneId.systemDefault())
        return sleepRepository.insertSampleData(sessions, tags) ?: "Added ${sessions.size} sample sleeps."
    }

    suspend fun removeSampleData(): String = "Removed ${sleepRepository.deleteSampleData()} sample sleeps."
}
