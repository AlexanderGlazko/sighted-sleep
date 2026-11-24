package com.sighted.sleep.ui.pages.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.domain.model.Targets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistoryRow(val session: SleepSession, val type: SessionType?)

/** Sessions grouped by the day they ended; date == null holds the active session. */
data class HistoryDay(val date: LocalDate?, val rows: List<HistoryRow>)

data class HistoryUiState(
    val loaded: Boolean = false,
    val days: List<HistoryDay> = emptyList(),
    val targets: Targets = Targets(),
    /** Tags that appear on at least one sleep, for the filter row. */
    val usedTags: List<Tag> = emptyList(),
    /** Active tag filter (spec F5.8), or null for all sleeps. */
    val filterTagId: Long? = null,
)

class HistoryViewModel(
    repository: SleepRepository,
    tagRepository: TagRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<Long?>(null)

    val state: StateFlow<HistoryUiState> = combine(
        repository.sessions,
        repository.sessionTags,
        tagRepository.tags,
        settings.targets,
        filter,
    ) { sessions, sessionTags, tags, targets, filterTagId ->
        // Classify on the full list so "main"/"nap" don't change when filtering.
        val types = SessionClassifier.classify(sessions)
        val usedIds = sessionTags.values.flatten().toSet()
        val visible = if (filterTagId == null) sessions else sessions.filter { filterTagId in sessionTags[it.id].orEmpty() }
        val days = visible
            .sortedByDescending { it.end ?: it.start }
            .groupBy { it.endDate }
            .map { (date, list) -> HistoryDay(date, list.map { HistoryRow(it, types[it.id]) }) }
        HistoryUiState(
            loaded = true,
            days = days,
            targets = targets,
            usedTags = tags.filter { it.id in usedIds },
            filterTagId = filterTagId?.takeIf { it in usedIds },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun toggleFilter(tagId: Long) {
        filter.value = if (filter.value == tagId) null else tagId
    }
}
