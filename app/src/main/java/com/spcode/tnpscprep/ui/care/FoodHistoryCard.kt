package com.spcode.tnpscprep.ui.care

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.FoodHistory
import com.spcode.tnpscprep.domain.FoodState
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.today.foodColor
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Food history: counts for the last 30 days + the last 5 weeks as coloured dots (a habit view: unlogged days are plain, never "bad"). */
@Composable
fun FoodHistoryCard(days: Map<LocalDate, FoodState>, today: LocalDate, onDay: (LocalDate) -> Unit = {}) {
    val s = FoodHistory.summary(days, today, 30)
    val weeks = FoodHistory.weeks(today, 5)
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.food_hist_summary, s.healthy, s.mixed, s.junk), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (d in 0..6) Text(
                    java.time.DayOfWeek.of(d + 1).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { d ->
                        val st = d?.let { days[it] }
                        Box(
                            Modifier.size(30.dp).clip(CircleShape)
                                .background(if (st != null) foodColor(st).copy(alpha = 0.35f) else Color.Transparent)
                                .border(BorderStroke(1.5.dp, if (d == null) Color.Transparent else if (st != null) foodColor(st) else MaterialTheme.colorScheme.outlineVariant), CircleShape)
                                .semantics { if (d != null) contentDescription = d.toString() }
                                .then(if (d != null) Modifier.clickable { onDay(d) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            // the day's food picture when logged, else the plain day number
                            if (st != null) com.spcode.tnpscprep.ui.today.FoodEmoji(st.name.lowercase(), 24.dp)
                            else if (d != null) Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
