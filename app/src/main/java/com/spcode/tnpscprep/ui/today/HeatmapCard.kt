package com.spcode.tnpscprep.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedButton
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
fun HeatmapSection(state: HeatState, food: Map<LocalDate, com.spcode.tnpscprep.domain.FoodState>, onDay: (String) -> Unit, weeks: Int = 12) {
    val shown = state.weeks.takeLast(weeks)
    val locale = java.util.Locale.getDefault()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.heat_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 6.dp),
        )
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val labelW = 16.dp
                    val gap = 4.dp
                    // the cells share the card width, so 12 weeks fill it without scrolling
                    val cell = ((maxWidth - labelW - gap * shown.size) / shown.size.coerceAtLeast(1)).coerceIn(12.dp, 28.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        // weekday labels: Monday, Wednesday, Friday
                        Column(Modifier.width(labelW), verticalArrangement = Arrangement.spacedBy(gap)) {
                            Spacer(Modifier.height(14.dp))
                            for (d in 0..6) {
                                Box(Modifier.height(cell), contentAlignment = androidx.compose.ui.Alignment.CenterStart) {
                                    if (d == 0 || d == 2 || d == 4) Text(
                                        java.time.DayOfWeek.of(d + 1).getDisplayName(java.time.format.TextStyle.NARROW, locale),
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            // month labels: shown where a new month starts
                            Row(Modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                shown.forEachIndexed { i, week ->
                                    val first = week.firstOrNull()?.date
                                    val prev = shown.getOrNull(i - 1)?.firstOrNull()?.date
                                    Box(Modifier.width(cell)) {
                                        if (first != null && (prev == null || prev.month != first.month)) {
                                            Text(
                                                first.month.getDisplayName(java.time.format.TextStyle.SHORT, locale), maxLines = 1, softWrap = false,
                                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.wrapContentWidth(align = androidx.compose.ui.Alignment.Start, unbounded = true),
                                            )
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                shown.forEach { week ->
                                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                        week.forEach { day ->
                                            if (day == null) Spacer(Modifier.size(cell))
                                            else Cell(day, hasNote = day.date in state.noteDates, food = food[day.date], size = cell, onClick = { onDay(day.date.toString()) })
                                        }
                                    }
                                }
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
private fun Cell(day: DaySummary, hasNote: Boolean, food: com.spcode.tnpscprep.domain.FoodState?, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val ring = if (hasNote) Modifier.background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)).padding(1.5.dp) else Modifier
    androidx.compose.foundation.layout.Box(ring) {
        androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.BottomEnd) {
            Spacer(
                Modifier.size(if (hasNote) size - 3.dp else size).clip(RoundedCornerShape(4.dp)).background(levelColor(day.level)).clickable(onClick = onClick),
            )
            // Small dot = that day's food check-in (colour + it is also listed with its name in the day sheet).
            if (food != null) Spacer(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(foodColor(food)))
        }
    }
}

/** Pop-up for one day: what she did (from activity_event) plus an optional one-line note. */
@Composable
fun DaySheet(vm: TodayViewModel, date: LocalDate, dismiss: () -> Unit, onDiary: (String) -> Unit) {
    val allEvents by vm.dayEvents(date).collectAsState(emptyList())
    val events = allEvents.filter { it.type != ActivityType.DIARY.name } // diary entries are shown as their own section below
    val diaryRepo = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp).container.diaryRepository
    val entries by remember(date) { diaryRepo.observeForDate(date) }.collectAsState(emptyList())
    val saved by vm.dayNote(date).collectAsState("")
    var text by remember(date) { mutableStateOf<String?>(null) }
    val shown = text ?: saved
    val minutes = events.sumOf { Heatmap.minutesOf(com.spcode.tnpscprep.domain.HeatEvent(it.localDate, it.type, it.details)) }

    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    if (minutes > 0) Text(stringResource(R.string.day_minutes, minutes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.height(12.dp))
    // "Day story": the diary first, then what she did (study sessions, food check-in, steps ...). A section with nothing in it is skipped.
    if (entries.isNotEmpty()) {
        Text(stringResource(R.string.diary_section), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { e ->
                Row(
                    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).clickable { onDiary("entry:" + e.id) }.padding(vertical = 6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Top,
                ) {
                    com.spcode.tnpscprep.ui.diary.DiaryMood(e.moodEmojiId, 32.dp)
                    Text(e.text.trim().take(160), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    if (events.isNotEmpty()) {
        if (entries.isNotEmpty()) Text(stringResource(R.string.diary_activity), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { events.forEach { EventRow(it) } }
    } else if (entries.isEmpty()) {
        Text(stringResource(R.string.day_nothing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = { onDiary("new:$date") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.diary_write_about)) }
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
private val FOOD_VALUE = Regex("\"value\"\\s*:\\s*\"(\\w+)\"")
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
        ActivityType.FOOD_CHECKIN.name -> stringResource(
            R.string.ev_food,
            stringResource(when (FOOD_VALUE.find(e.details)?.groupValues?.get(1)) { "HEALTHY" -> R.string.food_healthy; "JUNK" -> R.string.food_junk; else -> R.string.food_mixed }),
        )
        ActivityType.AFFAIR_ADDED.name -> stringResource(R.string.ev_affair_added)
        else -> stringResource(R.string.ev_other)
    }
    Row {
        // Backfilled rows only know the day, not a real time, so no clock is shown for them.
        Text(if (backfilled) "·" else time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
