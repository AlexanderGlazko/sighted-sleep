package com.sighted.sleep.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.domain.sample.SampleData
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
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val tagRepository: TagRepository,
    private val sleepRepository: SleepRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(settings.targets, settings.workDays, tagRepository.tags) { targets, workDays, tags ->
            SettingsUiState(loaded = true, targets = targets, workDays = workDays, tags = tags)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

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
