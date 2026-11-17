package com.spcode.tnpscprep.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.today.foodColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The full heatmap calendar: one month at a time (previous / next), each day coloured by the same study levels as the Today heatmap,
 * a dot for the food check-in and a ring where a diary entry exists. Tap a day = the day story (diary, then activity).
 */
@Composable
fun JournalCalendar(
    weeks: List<List<DaySummary?>>, food: Map<LocalDate, FoodState>, diaryDates: Set<LocalDate>, today: LocalDate,
    onDay: (LocalDate) -> Unit,
) {
    val levels = remember(weeks) { weeks.flatten().filterNotNull().associate { it.date to it.level } }
    val earliest = remember(weeks) { weeks.flatten().filterNotNull().minOfOrNull { it.date }?.let { YearMonth.from(it) } ?: YearMonth.from(today) }
    val thisMonth = YearMonth.from(today)
    var monthText by rememberSaveable { mutableStateOf(thisMonth.toString()) }
    val month = runCatching { YearMonth.parse(monthText) }.getOrDefault(thisMonth)
    val locale = Locale.getDefault()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalBottomBarInset.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monthText = month.minusMonths(1).toString() }, enabled = month.isAfter(earliest)) { Text("‹", style = MaterialTheme.typography.titleLarge) }
            Text(
                month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { monthText = month.plusMonths(1).toString() }, enabled = month.isBefore(thisMonth)) { Text("›", style = MaterialTheme.typography.titleLarge) }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    for (d in DayOfWeek.entries) Text(
                        d.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                }
                val lead = month.atDay(1).dayOfWeek.value - 1
                val cells = lead + month.lengthOfMonth()
                for (row in 0 until (cells + 6) / 7) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0..6) {
                            val n = row * 7 + col - lead + 1
                            if (n < 1 || n > month.lengthOfMonth()) { Box(Modifier.weight(1f).aspectRatio(1f)) ; continue }
                            val date = month.atDay(n)
                            DayCell(date, levels[date] ?: 0, food[date], date in diaryDates, future = date.isAfter(today), isToday = date == today, Modifier.weight(1f)) { onDay(date) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Text(stringResource(R.string.cal_legend), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.heat_less), style = MaterialTheme.typography.labelSmall)
                    (0..Heatmap.MAX_LEVEL).forEach { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(levelColor(it))) }
                    Text(stringResource(R.string.heat_more), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        // Reserved for the future music log (what she listened to while studying). Nothing is stored or shown yet.
        MusicLogSlot()
    }
}

@Composable
private fun levelColor(level: Int) =
    if (level <= 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.25f * (level - 1))

@Composable
private fun DayCell(date: LocalDate, level: Int, food: FoodState?, hasDiary: Boolean, future: Boolean, isToday: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val desc = date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
    Box(
        modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(if (future) androidx.compose.ui.graphics.Color.Transparent else levelColor(level))
            .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
            .then(if (!future) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium, color = if (future) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface)
        if (food != null) Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(6.dp).clip(CircleShape).background(foodColor(food)))
        if (hasDiary) Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(6.dp).border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape))
    }
}

/** SLOT for the future music log: intentionally draws nothing. Put the "what I listened to" card here when that feature exists. */
@Composable
fun MusicLogSlot() {}
