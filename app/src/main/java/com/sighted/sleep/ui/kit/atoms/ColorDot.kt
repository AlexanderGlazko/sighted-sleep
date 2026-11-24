package com.sighted.sleep.ui.kit.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A filled circle in [color]. Kit atom: knows nothing about tags or charts. */
@Composable
fun ColorDot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).background(color, CircleShape))
}
