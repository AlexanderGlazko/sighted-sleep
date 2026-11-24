package com.sighted.sleep.ui.features.timelineview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sighted.sleep.ui.kit.molecules.SegmentControl

/**
 * Switches the Timeline tab between the actogram and the session list (spec F5.2, F5.8).
 * Figma: Features / Switch timeline view / Segment control, View = Chart / List.
 */
@Composable
fun TimelineViewSwitch(
    showList: Boolean,
    onChange: (showList: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentControl(
        options = listOf("Chart", "List"),
        selectedIndex = if (showList) 1 else 0,
        onSelect = { onChange(it == 1) },
        modifier = modifier,
    )
}
