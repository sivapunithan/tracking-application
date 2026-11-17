package com.spcode.tnpscprep.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.data.db.ActivityEventEntity
import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.DaySummary
import com.spcode.tnpscprep.domain.Heatmap
import com.spcode.tnpscprep.ui.design.SoftCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
private fun levelColor(level: Int): Color {
    val c = MaterialTheme.colorScheme
    return if (level <= 0) c.onSurface.copy(alpha = 0.08f) else c.primary.copy(alpha = 0.25f + 0.25f * (level - 1))
}

/** GitHub-style grid: one column per week (Monday on top), newest week at the right edge. */
@Composable
fun HeatmapSection(state: HeatState, onDay: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.heat_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 6.dp),
        )
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val scroll = rememberScrollState()
                LaunchedEffect(state.weeks.size) { scroll.scrollTo(scroll.maxValue) } // show the newest weeks first
                Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { day ->
                                if (day == null) Spacer(Modifier.size(16.dp))
                                else Cell(day, hasNote = day.date in state.noteDates, onClick = { onDay(day.date.toString()) })
                            }
                        }
                    }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.heat_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.heat_less), style = MaterialTheme.typography.labelSmall)
                    (0..Heatmap.MAX_LEVEL).forEach { Spacer(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(levelColor(it))) }
                    Text(stringResource(R.string.heat_more), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun Cell(day: DaySummary, hasNote: Boolean, onClick: () -> Unit) {
    val ring = if (hasNote) Modifier.background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)).padding(1.5.dp) else Modifier
    androidx.compose.foundation.layout.Box(ring) {
        Spacer(
            Modifier.size(if (hasNote) 13.dp else 16.dp).clip(RoundedCornerShape(4.dp)).background(levelColor(day.level)).clickable(onClick = onClick),
        )
    }
}

/** Pop-up for one day: what she did (from activity_event) plus an optional one-line note. */
@Composable
fun DaySheet(vm: TodayViewModel, date: LocalDate, dismiss: () -> Unit) {
    val events by vm.dayEvents(date).collectAsState(emptyList())
    val saved by vm.dayNote(date).collectAsState("")
    var text by remember(date) { mutableStateOf<String?>(null) }
    val shown = text ?: saved
    val minutes = events.sumOf { Heatmap.minutesOf(com.spcode.tnpscprep.domain.HeatEvent(it.localDate, it.type, it.details)) }

    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    if (minutes > 0) Text(stringResource(R.string.day_minutes, minutes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.height(12.dp))
    if (events.isEmpty()) {
        Text(stringResource(R.string.day_nothing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { events.forEach { EventRow(it) } }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = shown, onValueChange = { text = it.take(200) }, modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.day_note_hint)) }, singleLine = true,
        keyboardOptions = KeyboardOptions.Default,
    )
    Spacer(Modifier.height(10.dp))
    Button(onClick = { vm.saveDayNote(date, shown); dismiss() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.day_note_save)) }
    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
}

private val INT_FROM = Regex("\"from\"\\s*:\\s*(\\d+)")
private val INT_STEPS = Regex("\"steps\"\\s*:\\s*(\\d+)")
private val INT_TO = Regex("\"to\"\\s*:\\s*(\\d+)")

@Composable
private fun EventRow(e: ActivityEventEntity) {
    val time = Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val backfilled = e.details.contains("backfilled")
    val label = when (e.type) {
        ActivityType.FOCUS_SESSION.name -> stringResource(R.string.ev_focus, Heatmap.minutesOf(com.spcode.tnpscprep.domain.HeatEvent(e.localDate, e.type, e.details)))
        ActivityType.TOPIC_PROGRESS.name -> stringResource(
            R.string.ev_topic, INT_FROM.find(e.details)?.groupValues?.get(1)?.toIntOrNull() ?: 0, INT_TO.find(e.details)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
        )
        ActivityType.STEPS.name -> stringResource(R.string.ev_steps, INT_STEPS.find(e.details)?.groupValues?.get(1)?.toIntOrNull() ?: 0)
        ActivityType.NOTE.name -> stringResource(R.string.ev_note)
        ActivityType.AFFAIR_REVISED.name -> stringResource(R.string.ev_affair_revised)
        ActivityType.AFFAIR_ADDED.name -> stringResource(R.string.ev_affair_added)
        else -> stringResource(R.string.ev_other)
    }
    Row {
        // Backfilled rows only know the day, not a real time, so no clock is shown for them.
        Text(if (backfilled) "·" else time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
