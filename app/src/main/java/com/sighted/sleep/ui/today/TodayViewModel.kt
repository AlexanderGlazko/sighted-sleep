package com.sighted.sleep.ui.today

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
    val latest: LatestSleep = LatestSleep(null, emptyList()),
    val targets: Targets = Targets(),
    val tags: List<Tag> = emptyList(),
    val sessionTags: Map<Long, Set<Long>> = emptyMap(),
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
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** A just-finished session shorter than 5 minutes, waiting for "discard or keep?". */
    var discardCandidate by mutableStateOf<SleepSession?>(null)
        private set

    var checkInDraft by mutableStateOf<CheckInDraft?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var lastWakeRequest = 0L

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

    /** Wake requested from the widget; [token] makes sure each request is handled once. */
    fun onWakeRequest(token: Long) {
        if (token == 0L || token == lastWakeRequest) return
        lastWakeRequest = token
        onWake()
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
