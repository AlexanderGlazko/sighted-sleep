package com.sighted.sleep.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.domain.model.Targets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class SettingsUiState(
    val loaded: Boolean = false,
    val targets: Targets = Targets(),
    val tags: List<Tag> = emptyList(),
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(settings.targets, tagRepository.tags) { targets, tags ->
        SettingsUiState(loaded = true, targets = targets, tags = tags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setBedtime(t: LocalTime) = viewModelScope.launch { settings.setBedtime(t) }
    fun setWake(t: LocalTime) = viewModelScope.launch { settings.setWake(t) }

    /** Deletes an unused custom tag. Returns an error message, or null on success. */
    suspend fun deleteTag(tag: Tag): String? = tagRepository.delete(tag)

    /** Creates (existing == null) or updates a tag. Returns an error message, or null on success. */
    suspend fun saveTag(existing: Tag?, name: String, colorHex: String, archived: Boolean): String? =
        if (existing == null) {
            tagRepository.add(name, colorHex)
        } else {
            tagRepository.update(existing.copy(name = name, colorHex = colorHex, isArchived = archived))
        }
}
