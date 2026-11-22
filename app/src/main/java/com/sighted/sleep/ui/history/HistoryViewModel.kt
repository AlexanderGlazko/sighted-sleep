package com.sighted.sleep.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistoryRow(val session: SleepSession, val type: SessionType?)

/** Sessions grouped by the day they ended; date == null holds the active session. */
data class HistoryDay(val date: LocalDate?, val rows: List<HistoryRow>)

data class HistoryUiState(val loaded: Boolean = false, val days: List<HistoryDay> = emptyList())

class HistoryViewModel(repository: SleepRepository) : ViewModel() {
    val state: StateFlow<HistoryUiState> = repository.sessions.map { sessions ->
        val types = SessionClassifier.classify(sessions)
        val days = sessions
            .sortedByDescending { it.end ?: it.start }
            .groupBy { it.endDate }
            .map { (date, list) -> HistoryDay(date, list.map { HistoryRow(it, types[it.id]) }) }
        HistoryUiState(loaded = true, days = days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
