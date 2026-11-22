package com.sighted.sleep.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.model.SessionRules
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.ui.message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class TodayUiState(
    val loaded: Boolean = false,
    val active: SleepSession? = null,
    val now: Instant = Instant.now(),
    val lastSleep: SleepSession? = null,
    val lastSleepType: SessionType? = null,
) {
    val activeDuration: Duration? get() = active?.duration(now)
    val stillSleepingWarning: Boolean
        get() = activeDuration?.let { it > SessionRules.STILL_SLEEPING_AFTER } ?: false
}

class TodayViewModel(private val repository: SleepRepository) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(repository.now())
            delay(1_000)
        }
    }

    val state: StateFlow<TodayUiState> =
        combine(repository.sessions, ticker) { sessions, now ->
            val finished = sessions.filter { !it.isActive }
            val last = finished.maxByOrNull { it.end!! }
            TodayUiState(
                loaded = true,
                active = sessions.firstOrNull { it.isActive },
                now = now,
                lastSleep = last,
                lastSleepType = last?.let { SessionClassifier.classify(finished)[it.id] },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** A just-finished session shorter than 5 minutes, waiting for "discard or keep?". */
    var discardCandidate by mutableStateOf<SleepSession?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onSleep() = viewModelScope.launch {
        errorMessage = repository.startSleep()?.message()
    }

    fun onWake() = viewModelScope.launch {
        val finished = repository.stopSleep() ?: return@launch
        if (finished.duration(finished.end!!) < SessionRules.MIN_DURATION) discardCandidate = finished
    }

    fun onDiscardConfirmed() = viewModelScope.launch {
        discardCandidate?.let { repository.delete(it.id) }
        discardCandidate = null
    }

    fun onKeepShortSession() {
        discardCandidate = null
    }

    fun onErrorShown() {
        errorMessage = null
    }
}
