package com.sighted.sleep.ui.pages.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.ui.features.sleeptype.TypeChoice
import com.sighted.sleep.data.SaveOutcome
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.domain.TargetMath
import com.sighted.sleep.domain.model.CheckIn
import com.sighted.sleep.domain.model.SessionType
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.ui.message
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class EditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    /** Only true when editing the currently running session. */
    val canBeActive: Boolean = false,
    val stillSleeping: Boolean = false,
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(23, 0),
    val endDate: LocalDate = LocalDate.now(),
    val endTime: LocalTime = LocalTime.of(7, 0),
    val type: TypeChoice = TypeChoice.AUTO,
    val checkIn: CheckIn = CheckIn(),
    val error: String? = null,
    val done: Boolean = false,
)

class EditSessionViewModel(
    private val repository: SleepRepository,
    tagRepository: TagRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long? = savedStateHandle.get<Long>(ARG_ID)?.takeIf { it >= 0 }
    private var zone: ZoneId = ZoneId.systemDefault()

    val tags: StateFlow<List<Tag>> =
        tagRepository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var state by mutableStateOf(EditUiState())
        private set

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val session = sessionId?.let { repository.getSession(it) }
        if (session == null) {
            // New session: default to the last finished target night (spec F4).
            val now = LocalDateTime.now(zone).truncatedTo(ChronoUnit.MINUTES)
            val (start, end) = TargetMath.defaultManualRange(now, settings.currentTargets())
            state = EditUiState(
                loaded = true,
                startDate = start.toLocalDate(), startTime = start.toLocalTime(),
                endDate = end.toLocalDate(), endTime = end.toLocalTime(),
            )
            return
        }
        zone = session.zone
        val start = session.start.atZone(zone).toLocalDateTime()
        val end = (session.end ?: repository.now()).atZone(zone).toLocalDateTime().truncatedTo(ChronoUnit.MINUTES)
        state = EditUiState(
            loaded = true,
            isNew = false,
            canBeActive = session.isActive,
            stillSleeping = session.isActive,
            startDate = start.toLocalDate(), startTime = start.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
            endDate = end.toLocalDate(), endTime = end.toLocalTime(),
            type = when (session.typeOverride) {
                SessionType.MAIN -> TypeChoice.MAIN
                SessionType.NAP -> TypeChoice.NAP
                null -> TypeChoice.AUTO
            },
            checkIn = repository.getCheckIn(session.id),
        )
    }

    fun setStartDate(d: LocalDate) = update { copy(startDate = d) }
    fun setStartTime(t: LocalTime) = update { copy(startTime = t) }
    fun setEndDate(d: LocalDate) = update { copy(endDate = d) }
    fun setEndTime(t: LocalTime) = update { copy(endTime = t) }
    fun setType(t: TypeChoice) = update { copy(type = t) }
    fun setStillSleeping(v: Boolean) = update { copy(stillSleeping = v) }
    fun setCheckIn(c: CheckIn) = update { copy(checkIn = c) }

    /** Duration preview, or null if the times are invalid. */
    fun previewDuration(): Duration? {
        if (state.stillSleeping) return null
        val d = Duration.between(startInstant(), endInstant())
        return d.takeIf { !it.isNegative && !it.isZero }
    }

    fun save() = viewModelScope.launch {
        val override = when (state.type) {
            TypeChoice.AUTO -> null
            TypeChoice.MAIN -> SessionType.MAIN
            TypeChoice.NAP -> SessionType.NAP
        }
        val end = if (state.stillSleeping) null else endInstant()
        state = when (val outcome = repository.save(sessionId, startInstant(), end, override)) {
            is SaveOutcome.Invalid -> state.copy(error = outcome.error.message())
            is SaveOutcome.Saved -> {
                repository.saveCheckIn(outcome.id, state.checkIn)
                state.copy(done = true)
            }
        }
    }

    fun delete() = viewModelScope.launch {
        sessionId?.let { repository.delete(it) }
        state = state.copy(done = true)
    }

    private fun startInstant() = state.startDate.atTime(state.startTime).atZone(zone).toInstant()
    private fun endInstant() = state.endDate.atTime(state.endTime).atZone(zone).toInstant()

    private inline fun update(block: EditUiState.() -> EditUiState) {
        state = state.block().copy(error = null)
    }

    companion object {
        const val ARG_ID = "id"
    }
}
