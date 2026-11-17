package com.spcode.tnpscprep.ui.diary

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.DaySummary
import com.spcode.tnpscprep.domain.FoodState
import com.spcode.tnpscprep.domain.Heatmap
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.today.foodColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The Journal's calendar: a one-week strip by default (the days around the selected one) that opens into the full month.
 * Each day shows its study level as a tint, the food dot, and the MOOD PICTURE of that day's diary entry (it pops when the day is selected).
 * Tapping a day selects it; the day story below the card shows what happened.
 */
@Composable
fun JournalCalendarCard(
    weeks: List<List<DaySummary?>>, food: Map<LocalDate, FoodState>, moods: Map<LocalDate, String>, diaryDates: Set<LocalDate>,
    today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val levels = remember(weeks) { weeks.flatten().filterNotNull().associate { it.date to it.level } }
    val earliest = remember(weeks) { weeks.flatten().filterNotNull().minOfOrNull { it.date } ?: today }
    val locale = Locale.getDefault()
    val monthOf = YearMonth.from(selected)
    val weekStart = selected.minusDays((selected.dayOfWeek.value - 1).toLong())

    fun go(target: LocalDate) { if (!target.isBefore(earliest)) onSelect(if (target.isAfter(today)) today else target) }
    val canBack = if (expanded) !monthOf.minusMonths(1).atEndOfMonth().isBefore(earliest) else !weekStart.minusDays(1).isBefore(earliest)
    val canForward = if (expanded) monthOf.isBefore(YearMonth.from(today)) else weekStart.plusDays(7).let { !it.isAfter(today) }
    val label = if (expanded) monthOf.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
    else weekStart.format(DateTimeFormatter.ofPattern("d MMM", locale)) + " – " + weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM", locale))

    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { go(if (expanded) selected.minusMonths(1) else selected.minusWeeks(1)) }, enabled = canBack) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                TextButton(onClick = { go(if (expanded) selected.plusMonths(1) else selected.plusWeeks(1)) }, enabled = canForward) { Text("›", style = MaterialTheme.typography.titleLarge) }
                TextButton(onClick = { expanded = !expanded }) { Text(stringResource(if (expanded) R.string.cal_week else R.string.cal_month)) }
            }
            Row(Modifier.fillMaxWidth()) {
                for (d in DayOfWeek.entries) Text(d.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            if (!expanded) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0..6) {
                        val d = weekStart.plusDays(i.toLong())
                        DayCell(d, levels[d] ?: 0, food[d], moods[d], d in diaryDates, d == selected, d.isAfter(today), d == today, big = true, Modifier.weight(1f)) { onSelect(d) }
                    }
                }
            } else {
                val lead = monthOf.atDay(1).dayOfWeek.value - 1
                val cells = lead + monthOf.lengthOfMonth()
                for (row in 0 until (cells + 6) / 7) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0..6) {
                            val n = row * 7 + col - lead + 1
                            if (n < 1 || n > monthOf.lengthOfMonth()) { Box(Modifier.weight(1f).aspectRatio(1f)); continue }
                            val d = monthOf.atDay(n)
                            DayCell(d, levels[d] ?: 0, food[d], moods[d], d in diaryDates, d == selected, d.isAfter(today), d == today, big = false, Modifier.weight(1f)) { onSelect(d) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.cal_legend), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.heat_less), style = MaterialTheme.typography.labelSmall)
                    (0..Heatmap.MAX_LEVEL).forEach { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(levelColor(it))) }
                    Text(stringResource(R.string.heat_more), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun levelColor(level: Int) =
    if (level <= 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.25f * (level - 1))

@Composable
private fun DayCell(
    date: LocalDate, level: Int, food: FoodState?, mood: String?, hasDiary: Boolean, selected: Boolean, future: Boolean, isToday: Boolean,
    big: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val desc = date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
    // the selected day pops (spring overshoot): the mood picture pops a little more
    val pop by animateFloatAsState(if (selected) 1.06f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "dayPop")
    val emojiPop by animateFloatAsState(if (selected) 1.3f else 1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium), label = "emojiPop")
    Box(
        modifier.then(if (big) Modifier.height(66.dp) else Modifier.aspectRatio(1f)).graphicsLayer { scaleX = pop; scaleY = pop }
            .clip(RoundedCornerShape(10.dp)).background(if (future) Color.Transparent else levelColor(level))
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)) else Modifier)
            .then(if (!future) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium,
            color = if (future) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(if (big) Alignment.TopCenter else Alignment.Center).padding(top = if (big) 5.dp else 0.dp),
        )
        if (mood != null) Box(Modifier.align(if (big) Alignment.BottomCenter else Alignment.BottomEnd).padding(bottom = if (big) 3.dp else 1.dp, end = if (big) 0.dp else 1.dp).graphicsLayer { scaleX = emojiPop; scaleY = emojiPop }) {
            DiaryMood(mood, if (big) 30.dp else 16.dp)
        } else if (hasDiary) Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(6.dp).border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape))
        if (food != null) Box(Modifier.align(Alignment.BottomStart).padding(3.dp).size(6.dp).clip(CircleShape).background(foodColor(food)))
    }
}
