package com.sighted.sleep.ui.features.edittiming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sighted.sleep.ui.dayText
import com.sighted.sleep.ui.kit.organisms.DateDialog
import com.sighted.sleep.ui.kit.organisms.TimeDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * "Fell asleep" / "Woke up": a date button and a time button that open the pickers (spec F1.3, F1.4).
 * Figma: the Start / End blocks of the Timing section on Edit sleep.
 */
@Composable
fun DateTimeField(
    label: String,
    date: LocalDate,
    time: LocalTime,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) { Text(date.dayText()) }
            OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) { Text(time.format(timeFormat)) }
        }
    }

    if (showDate) {
        DateDialog(date, onDismiss = { showDate = false }, onConfirm = { onDate(it); showDate = false })
    }
    if (showTime) {
        TimeDialog(time, onDismiss = { showTime = false }, onConfirm = { onTime(it); showTime = false })
    }
}
