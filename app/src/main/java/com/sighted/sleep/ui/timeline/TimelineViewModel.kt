package com.sighted.sleep.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.metrics.DaySegment
import com.sighted.sleep.domain.metrics.SleepMetrics
import com.sighted.sleep.domain.model.Targets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/** One actogram row: a calendar day with its sleep pieces (spec F5.2). */
data class ActogramRow(val date: LocalDate, val segments: List<DaySegment>, val isWorkDay: Boolean)

data class TimelineUiState(
    val loaded: Boolean = false,
    /** Newest first, every day from the first logged sleep to today (empty days included). */
    val rows: List<ActogramRow> = emptyList(),
    val targets: Targets = Targets(),
)

class TimelineViewModel(repository: SleepRepository, settings: SettingsRepository) : ViewModel() {

    // Refresh once a minute so a running sleep keeps growing on the chart.
    private val minuteTicker = flow {
        while (true) {
            emit(repository.now())
            delay(60_000)
        }
    }

    val state: StateFlow<TimelineUiState> = combine(
        repository.sessions,
        settings.targets,
        settings.workDays,
        minuteTicker,
    ) { sessions, targets, workDays, now ->
        val segments = SleepMetrics.daySegments(sessions, SessionClassifier.classify(sessions), now)
        val today = LocalDate.now(ZoneId.systemDefault())
        val first = segments.keys.minOrNull()
        val rows = if (first == null) emptyList() else {
            generateSequence(today) { it.minusDays(1) }
                .takeWhile { !it.isBefore(first) }
                .map { d -> ActogramRow(d, segments[d].orEmpty(), d.dayOfWeek in workDays) }
                .toList()
        }
        TimelineUiState(loaded = true, rows = rows, targets = targets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())
}
