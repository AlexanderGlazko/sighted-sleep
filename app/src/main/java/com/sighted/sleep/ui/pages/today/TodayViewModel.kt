package com.sighted.sleep.ui.pages.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.domain.LatestSleep
import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.latestSleep
import com.sighted.sleep.domain.model.CheckIn
import com.sighted.sleep.domain.model.SessionRules
import com.sighted.sleep.domain.model.SleepSession
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.ui.message
import com.sighted.sleep.reminders.AppRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.sighted.sleep.domain.metrics.SleepMetrics
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class TodayUiState(
    val loaded: Boolean = false,
    val active: SleepSession? = null,
    val now: Instant = Instant.now(),
    val latest: LatestSleep = LatestSleep(null, emptyList()),
    val targets: Targets = Targets(),
    val tags: List<Tag> = emptyList(),
    val sessionTags: Map<Long, Set<Long>> = emptyMap(),
    /** Rolling 7-day debt vs. target (spec F5.1). */
    val sleepDebt: Duration = Duration.ZERO,
) {
    val activeDuration: Duration? get() = active?.duration(now)
    val stillSleepingWarning: Boolean
        get() = activeDuration?.let { it > SessionRules.STILL_SLEEPING_AFTER } ?: false
}

/** The morning check-in being filled in for one session. */
data class CheckInDraft(val session: SleepSession, val checkIn: CheckIn)

class TodayViewModel(
    private val repository: SleepRepository,
    tagRepository: TagRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(repository.now())
            delay(1_000)
        }
    }

    val state: StateFlow<TodayUiState> =
        combine(
            repository.sessions,
            ticker,
            settings.targets,
            tagRepository.tags,
            repository.sessionTags,
        ) { sessions, now, targets, tags, sessionTags ->
            TodayUiState(
                loaded = true,
                active = sessions.firstOrNull { it.isActive },
                now = now,
                latest = latestSleep(sessions, SessionClassifier.classify(sessions)),
                targets = targets,
                tags = tags,
                sessionTags = sessionTags,
                sleepDebt = SleepMetrics.sleepDebt(SleepMetrics.dailyTotals(sessions), targets.duration, LocalDate.now()),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** A just-finished session shorter than 5 minutes, waiting for "discard or keep?". */
    var discardCandidate by mutableStateOf<SleepSession?>(null)
        private set

    var checkInDraft by mutableStateOf<CheckInDraft?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var lastRequestToken = 0L

    fun onSleep() = viewModelScope.launch {
        errorMessage = repository.startSleep()?.message()
    }

    fun onWake() = viewModelScope.launch {
        val finished = repository.stopSleep() ?: return@launch
        if (finished.duration(finished.end!!) < SessionRules.MIN_DURATION) {
            discardCandidate = finished
        } else {
            openCheckIn(finished.id)
        }
    }

    /** Request from the widget or a notification; each token is handled once. */
    fun onRequest(request: AppRequest) {
        if (request.token == lastRequestToken) return
        lastRequestToken = request.token
        when (request.kind) {
            AppRequest.Kind.WAKE -> onWake()
            AppRequest.Kind.CHECK_IN -> request.sessionId?.let { openCheckIn(it) }
            AppRequest.Kind.EDIT -> Unit // handled by navigation
        }
    }

    fun onDiscardConfirmed() = viewModelScope.launch {
        discardCandidate?.let { repository.delete(it.id) }
        discardCandidate = null
    }

    fun onKeepShortSession() {
        discardCandidate = null
    }

    fun openCheckIn(sessionId: Long) = viewModelScope.launch {
        val session = repository.getSession(sessionId) ?: return@launch
        checkInDraft = CheckInDraft(session, repository.getCheckIn(sessionId))
    }

    fun updateCheckIn(checkIn: CheckIn) {
        checkInDraft = checkInDraft?.copy(checkIn = checkIn)
    }

    fun saveCheckIn() = viewModelScope.launch {
        val draft = checkInDraft ?: return@launch
        repository.saveCheckIn(draft.session.id, draft.checkIn)
        checkInDraft = null
    }

    fun skipCheckIn() {
        checkInDraft = null
    }

    fun onErrorShown() {
        errorMessage = null
    }
}
