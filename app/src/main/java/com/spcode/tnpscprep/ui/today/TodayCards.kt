package com.spcode.tnpscprep.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.GettingStartedItem
import com.spcode.tnpscprep.domain.GettingStartedStep
import com.spcode.tnpscprep.domain.NextAction
import com.spcode.tnpscprep.data.db.PlannedTaskEntity
import com.spcode.tnpscprep.ui.design.ProgressRing
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.design.animatedInt
import com.spcode.tnpscprep.ui.design.pressScale
import com.spcode.tnpscprep.ui.diary.DiaryMood
import com.spcode.tnpscprep.ui.localized
import com.spcode.tnpscprep.ui.theme.LocalIsDark

/** The one card that says what to do next (rule order: domain/TodayGuide). Mood pictures are the empty-state art. */
@Composable
fun NextActionCard(action: NextAction, firstDue: DueItem?, nextTopicName: String?, onAction: () -> Unit) {
    val dark = LocalIsDark.current
    val brush = Brush.linearGradient(if (dark) listOf(Color(0xFF9B0F26), Color(0xFF4A0A14)) else listOf(Color(0xFFC8102E), Color(0xFF7A0019)))
    val title: String
    val body: String
    val cta: String?
    val art: String
    when (action) {
        NextAction.SetExamDate -> { title = stringResource(R.string.na_set_exam_title); body = stringResource(R.string.na_set_exam_body); cta = stringResource(R.string.set_exam_date); art = "PROUD" }
        is NextAction.Revise -> {
            title = stringResource(R.string.na_revise_title, firstDue?.let { localized(it.nameEn, it.nameTa) }.orEmpty())
            body = pluralStringResource(R.plurals.na_revise_body, action.dueCount, action.dueCount)
            cta = stringResource(R.string.na_revise_cta); art = "WORRIED"
        }
        NextAction.StartFocus -> { title = stringResource(R.string.na_focus_title); body = stringResource(R.string.na_focus_body); cta = stringResource(R.string.na_focus_cta); art = "CHEER" }
        NextAction.LogFood -> { title = stringResource(R.string.food_question); body = stringResource(R.string.na_food_body); cta = stringResource(R.string.na_food_cta); art = "LAUGH" }
        is NextAction.PlannedTask -> { title = stringResource(R.string.na_planned_title); body = action.title; cta = stringResource(R.string.na_planned_cta); art = "PROUD" }
        NextAction.NewTopic -> { title = stringResource(R.string.na_topic_title); body = nextTopicName.orEmpty(); cta = stringResource(R.string.na_topic_cta); art = "PROUD" }
        NextAction.AllCaughtUp -> { title = stringResource(R.string.na_done_title); body = stringResource(R.string.na_done_body); cta = null; art = "CELEBRATE" }
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(brush).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.na_label), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 3)
                if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), maxLines = 3)
                if (cta != null) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Color.White).pressScale(onClick = onAction).padding(horizontal = 18.dp, vertical = 10.dp),
                    ) { Text(cta, style = MaterialTheme.typography.labelLarge, color = Color(0xFF7A0019)) }
                }
            }
            Spacer(Modifier.width(12.dp))
            DiaryMood(art, 72.dp)
        }
    }
}

/** Compact first-run strip (replaces the big checklist card): dots for the four steps + the next undone one. Tap = do it. */
@Composable
fun GettingStartedStrip(items: List<GettingStartedItem>, onStep: (GettingStartedStep) -> Unit) {
    val next = items.firstOrNull { !it.done } ?: return
    SoftCard(Modifier.fillMaxWidth(), onClick = { onStep(next.step) }) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.gs_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    items.forEach { Box(Modifier.size(8.dp).clip(CircleShape).background(if (it.done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant)) }
                }
                Text(
                    stringResource(
                        when (next.step) {
                            GettingStartedStep.EXAM_DATE -> R.string.gs_exam
                            GettingStartedStep.FIRST_FOCUS -> R.string.gs_focus
                            GettingStartedStep.FIRST_FOOD -> R.string.gs_food
                            GettingStartedStep.FIRST_DIARY -> R.string.gs_diary
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** First-run checklist: disappears for good once all four steps are done. */
@Composable
fun GettingStartedCard(items: List<GettingStartedItem>, onStep: (GettingStartedStep) -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.gs_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(R.string.gs_sub, items.count { it.done }, items.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DiaryMood("CHEER", 52.dp)
            }
            items.forEach { it ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).then(if (!it.done) Modifier.clickable { onStep(it.step) } else Modifier).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (it.done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                        tint = if (it.done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(
                            when (it.step) {
                                GettingStartedStep.EXAM_DATE -> R.string.gs_exam
                                GettingStartedStep.FIRST_FOCUS -> R.string.gs_focus
                                GettingStartedStep.FIRST_FOOD -> R.string.gs_food
                                GettingStartedStep.FIRST_DIARY -> R.string.gs_diary
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (it.done) TextDecoration.LineThrough else null,
                        color = if (it.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * Today's plan: the day's STORED list (planned_task), at most three lines. A progress ring counts only these lines; ticking one does its action
 * (revise / read / review) and it stays in the list, ticked. A line also shows as done when she did that work elsewhere (e.g. in the Study tab).
 */
@Composable
fun PlanCard(tasks: List<PlannedTaskEntity>, doneRefs: Set<String>, onTick: (PlannedTaskEntity) -> Unit) {
    fun isDone(t: PlannedTaskEntity) = t.done || (t.refId != null && t.refId in doneRefs)
    val doneNow = tasks.count(::isDone)
    val total = tasks.size
    val allDone = total > 0 && doneNow == total
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.plan_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        stringResource(if (allDone) R.string.plan_complete else R.string.plan_left, total - doneNow),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ProgressRing(
                    (doneNow.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f), Modifier.size(52.dp), strokeWidth = 6.dp,
                    track = MaterialTheme.colorScheme.outlineVariant, color = MaterialTheme.colorScheme.primary,
                ) { Text("$doneNow/$total", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1) }
            }
            Spacer(Modifier.height(4.dp))
            tasks.forEach { t ->
                val done = isDone(t)
                val kind = when (t.refType) { "REVISION" -> R.string.plan_kind_review; "TOPIC" -> R.string.plan_kind_read; "CURRENT_AFFAIRS" -> R.string.plan_kind_news; else -> R.string.plan_kind_custom }
                val icon = when (t.refType) { "REVISION" -> Icons.Rounded.Replay; "TOPIC" -> Icons.Rounded.MenuBook; "CURRENT_AFFAIRS" -> Icons.Rounded.Newspaper; else -> Icons.Rounded.RadioButtonUnchecked }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (done) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .clickable(enabled = !done) { onTick(t) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (done) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (done) Icons.Rounded.CheckCircle else icon, null, modifier = Modifier.size(22.dp), tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(kind), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            t.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** Three tiles: a wide study tile (ring + minutes of goal + last 7 days), streak, steps. [week] = study minutes for the last 7 days, oldest first. */
@Composable
fun TileRow(ui: TodayUiState, stepsToday: Int?, week: List<Int> = emptyList()) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
        SoftCard(Modifier.weight(1.7f).fillMaxHeight()) {
            Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(
                        (ui.todayMinutes.toFloat() / ui.goalMinutes).coerceIn(0f, 1f), Modifier.size(64.dp), strokeWidth = 8.dp,
                        track = MaterialTheme.colorScheme.outlineVariant, color = MaterialTheme.colorScheme.secondary,
                    ) { Text(animatedInt(ui.todayMinutes).toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.tile_study_title), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.tile_study_of, ui.todayMinutes, ui.goalMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (week.isNotEmpty()) {
                    val max = maxOf(week.max(), ui.goalMinutes, 1)
                    Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                        week.forEachIndexed { i, m ->
                            Box(
                                Modifier.weight(1f).height((4 + 24f * m / max).dp).clip(RoundedCornerShape(3.dp))
                                    .background(if (i == week.lastIndex) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = if (m > 0) 0.45f else 0.15f)),
                            )
                        }
                    }
                }
            }
        }
        SoftCard(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (ui.streak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                }
                Text(stringResource(R.string.tile_streak, ui.streak), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(
                        stepsToday?.let { "~$it" } ?: "–", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1,
                    )
                }
                Text(stringResource(R.string.tile_steps), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
