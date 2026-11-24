package com.sighted.sleep.ui.features.togglesleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sighted.sleep.ui.kit.atoms.LargeToggleButton

/**
 * Starts or ends a sleep (spec F1.1). Figma: Features / Toggle sleep / Button, State = Awake / Sleeping.
 */
@Composable
fun ToggleSleepButton(
    sleeping: Boolean,
    onSleep: () -> Unit,
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LargeToggleButton(
        label = if (sleeping) "Wake up" else "Go to sleep",
        on = sleeping,
        onClick = if (sleeping) onWake else onSleep,
        modifier = modifier,
    )
}
