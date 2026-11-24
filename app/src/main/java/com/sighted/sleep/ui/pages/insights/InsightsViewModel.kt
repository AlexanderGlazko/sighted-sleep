package com.sighted.sleep.ui.pages.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sighted.sleep.data.SettingsRepository
import com.sighted.sleep.data.SleepRepository
import com.sighted.sleep.data.TagRepository
import com.sighted.sleep.domain.SessionClassifier
import com.sighted.sleep.domain.metrics.DateRange
import com.sighted.sleep.domain.metrics.Period
import com.sighted.sleep.domain.metrics.PeriodStats
import com.sighted.sleep.domain.metrics.PeriodSummary
import com.sighted.sleep.domain.metrics.Periods
import com.sighted.sleep.domain.metrics.SleepMetrics
import com.sighted.sleep.domain.metrics.TrendPoint
import com.sighted.sleep.domain.model.Targets
import com.sighted.sleep.domain.model.Tag
import com.sighted.sleep.domain.metrics.BedtimeBucket
import com.sighted.sleep.domain.metrics.InsightMetrics
import com.sighted.sleep.domain.metrics.MainSleep
import com.sighted.sleep.domain.metrics.TagImpact
import com.sighted.sleep.ui.dayText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class InsightsUiState(
    val loaded: Boolean = false,
    val period: Period = Period.WEEK,
    val label: String = "",
    val canGoNext: Boolean = false,
    val summary: PeriodSummary? = null,
    /** The period before, for "vs last week" deltas (spec F5.7). */
    val previous: PeriodSummary? = null,
    val trend: List<TrendPoint> = emptyList(),
    val targets: Targets = Targets(),
    /** Quality by bedtime in this period (spec F5.5). */
    val bedtimeBuckets: List<BedtimeBucket> = emptyList(),
    val bestBedtime: BedtimeBucket? = null,
    /** With vs. without each tag in this period (spec F5.6). */
    val tagImpacts: List<Pair<Tag, TagImpact>> = emptyList(),
)

private data class Inputs(
    val mains: List<MainSleep>,
    val totals: Map<LocalDate, java.time.Duration>,
    val targets: Targets,
    val tags: List<Tag>,
)

private data class Selection(val period: Period, val anchor: LocalDate)

class InsightsViewModel(
    repository: SleepRepository,
    tagRepository: TagRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection(Period.WEEK, LocalDate.now()))

    private val inputs = combine(
        repository.sessions,
        repository.sessionTags,
        tagRepository.tags,
        settings.targets,
        settings.workDays,
    ) { sessions, sessionTags, tags, targets, workDays ->
        val dayOff = tags.firstOrNull { it.isPreset && it.name == "Day off" }?.id
        val mains = SleepMetrics.mainSleeps(sessions, SessionClassifier.classify(sessions), sessionTags, workDays, dayOff)
        Inputs(mains, SleepMetrics.dailyTotals(sessions), targets, tags)
    }

    val state: StateFlow<InsightsUiState> = combine(inputs, selection) { (mains, totals, targets, tags), sel ->
        val today = LocalDate.now()
        val range = Periods.range(sel.period, sel.anchor)
        val prevRange = Periods.range(sel.period, Periods.shift(sel.period, sel.anchor, -1))
        val inRange = mains.filter { it.date in range }
        val buckets = InsightMetrics.qualityByBedtime(inRange)
        val tagsById = tags.associateBy { it.id }
        InsightsUiState(
            loaded = true,
            period = sel.period,
            label = label(sel.period, range, today),
            canGoNext = !Periods.range(sel.period, Periods.shift(sel.period, sel.anchor, 1)).start.isAfter(today),
            summary = PeriodStats.summary(range, mains, totals, targets.duration, today),
            previous = PeriodStats.summary(prevRange, mains, totals, targets.duration, today)
                .takeIf { it.loggedDays > 0 },
            trend = PeriodStats.trend(sel.period, range, mains, totals),
            targets = targets,
            bedtimeBuckets = buckets,
            bestBedtime = InsightMetrics.bestBedtime(buckets),
            tagImpacts = InsightMetrics.tagImpact(inRange, tagsById.keys)
                .mapNotNull { impact -> tagsById[impact.tagId]?.let { it to impact } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun setPeriod(p: Period) {
        selection.value = Selection(p, LocalDate.now())
    }

    fun previous() {
        val s = selection.value
        selection.value = s.copy(anchor = Periods.shift(s.period, s.anchor, -1))
    }

    fun next() {
        if (!state.value.canGoNext) return
        val s = selection.value
        selection.value = s.copy(anchor = Periods.shift(s.period, s.anchor, 1))
    }

    private fun label(period: Period, range: DateRange, today: LocalDate): String = when (period) {
        Period.DAY -> range.start.dayText(today)
        Period.WEEK -> if (today in range) "This week" else
            "${range.start.format(DateTimeFormatter.ofPattern("d MMM"))} – ${range.end.format(DateTimeFormatter.ofPattern("d MMM"))}"
        Period.MONTH -> range.start.format(DateTimeFormatter.ofPattern("LLLL yyyy"))
        Period.YEAR -> range.start.year.toString()
    }
}
