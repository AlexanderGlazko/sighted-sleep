package com.sighted.sleep.ui.features.period

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sighted.sleep.domain.metrics.Period
import com.sighted.sleep.ui.kit.molecules.SegmentControl

/** Day · Week · Month · Year for Insights (spec F5.3). Figma: Features / Pick period / Segment control. */
@Composable
fun PeriodSwitch(
    period: Period,
    onChange: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    val periods = listOf(Period.DAY to "Day", Period.WEEK to "Week", Period.MONTH to "Month", Period.YEAR to "Year")
    SegmentControl(
        options = periods.map { it.second },
        selectedIndex = periods.indexOfFirst { it.first == period },
        onSelect = { onChange(periods[it].first) },
        modifier = modifier.fillMaxWidth(),
    )
}
