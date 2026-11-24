package com.sighted.sleep.ui.kit.atoms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Full-width 96 dp toggle button (Figma: Atoms / Large toggle button, State = Off / On).
 * Off uses the secondary container colours, On the primary colours. The label comes from the caller.
 */
@Composable
fun LargeToggleButton(
    label: String,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        colors = if (on) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}
