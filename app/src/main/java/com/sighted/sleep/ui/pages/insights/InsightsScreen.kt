package com.sighted.sleep.ui.pages.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sighted.sleep.domain.metrics.Period
import com.sighted.sleep.ui.AppViewModels
import com.sighted.sleep.ui.features.period.PeriodSwitch
import com.sighted.sleep.ui.kit.molecules.StepperHeader
import com.sighted.sleep.ui.widgets.charts.BestBedtimeCard
import com.sighted.sleep.ui.widgets.charts.TagImpactCard
import com.sighted.sleep.ui.widgets.charts.TrendCharts
import com.sighted.sleep.ui.widgets.stats.SummaryStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    bottomBar: @Composable () -> Unit,
    viewModel: InsightsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Insights") }) }, bottomBar = bottomBar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PeriodSwitch(state.period, onChange = viewModel::setPeriod)
            StepperHeader(
                title = state.label,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
                canGoNext = state.canGoNext,
            )

            val summary = state.summary ?: return@Column
            if (summary.loggedDays == 0) {
                Text(
                    "No sleep logged in this period.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            SummaryStats(summary, state.previous, state.targets, state.period)

            if (state.period != Period.DAY && state.trend.isNotEmpty()) {
                TrendCharts(state.trend, state.period, state.targets)
            }

            if (state.period != Period.DAY) {
                BestBedtimeCard(state.bedtimeBuckets, state.bestBedtime)
                TagImpactCard(state.tagImpacts)
            }
        }
    }
}
