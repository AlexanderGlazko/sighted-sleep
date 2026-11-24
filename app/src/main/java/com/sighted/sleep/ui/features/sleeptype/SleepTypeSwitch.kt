package com.sighted.sleep.ui.features.sleeptype

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sighted.sleep.ui.kit.molecules.SegmentControl

/** Sleep type as the user picks it: Auto lets the classifier decide (spec §3.2, F1.5). */
enum class TypeChoice { AUTO, MAIN, NAP }

/** Auto · Main · Nap. Figma: Features / Set sleep type / Segment control, Type = Auto / Main / Nap. */
@Composable
fun SleepTypeSwitch(
    type: TypeChoice,
    onChange: (TypeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choices = listOf(TypeChoice.AUTO to "Auto", TypeChoice.MAIN to "Main", TypeChoice.NAP to "Nap")
    SegmentControl(
        options = choices.map { it.second },
        selectedIndex = choices.indexOfFirst { it.first == type },
        onSelect = { onChange(choices[it].first) },
        modifier = modifier.fillMaxWidth(),
    )
}
