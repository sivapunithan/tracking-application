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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.GettingStartedItem
import com.spcode.tnpscprep.domain.GettingStartedStep
import com.spcode.tnpscprep.domain.NextAction
import com.spcode.tnpscprep.domain.PlanItem
import com.spcode.tnpscprep.domain.PlanSummary
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

/** Today's plan: 3-5 checkable items from existing data. Tapping the circle does the action (revise / read / review). */
@Composable
fun PlanCard(
    plan: PlanSummary, due: Map<String, DueItem>, newTopicName: String?, affairTitle: String?,
    onRevise: (DueItem) -> Unit, onReadTopic: (String) -> Unit, onReviewAffair: (String) -> Unit,
) {
    var checked by remember { mutableStateOf(setOf<String>()) }
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.plan_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.plan_progress, plan.done, plan.total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            plan.items.forEach { item ->
                val key: String
                val label: String
                val act: () -> Unit
                when (item) {
                    is PlanItem.Revise -> {
                        val d = due[item.id]
                        key = "r-" + item.id
                        label = stringResource(R.string.plan_revise, d?.let { localized(it.nameEn, it.nameTa) }.orEmpty())
                        act = { d?.let(onRevise) }
                    }
                    is PlanItem.NewTopic -> { key = "t-" + item.id; label = stringResource(R.string.plan_topic, newTopicName.orEmpty()); act = { onReadTopic(item.id) } }
                    is PlanItem.ReviewAffair -> { key = "a-" + item.id; label = stringResource(R.string.plan_affair, affairTitle.orEmpty()); act = { onReviewAffair(item.id) } }
                }
                val done = key in checked
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(enabled = !done) { checked = checked + key; act() }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                        tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        label, style = MaterialTheme.typography.bodyLarge, maxLines = 2,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** One-tap water: 250 ml per tap, written as a health entry (the same storage the Care tab reads). The mood check-in has a reserved slot below. */
@Composable
fun WaterRow(todayMl: Int, onAdd: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WaterDrop, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.water_label), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.health_ml, todayMl), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            OutlinedButton(onClick = onAdd) { Text(stringResource(R.string.water_add)) }
        }
        // SLOT (future): the one-tap MOOD check-in goes here once it has storage.
    }
}

/** Three compact tiles: study ring, streak, steps. */
@Composable
fun TileRow(ui: TodayUiState, stepsToday: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SoftCard(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProgressRing(
                    (ui.todayMinutes.toFloat() / ui.goalMinutes).coerceIn(0f, 1f), Modifier.size(54.dp), strokeWidth = 7.dp,
                    track = MaterialTheme.colorScheme.outlineVariant, color = MaterialTheme.colorScheme.secondary,
                ) { Text(animatedInt(ui.todayMinutes).toString(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
                Text(stringResource(R.string.tile_study, ui.goalMinutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SoftCard(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (ui.streak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(34.dp))
                }
                Text(stringResource(R.string.tile_streak, ui.streak), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SoftCard(Modifier.weight(1f)) {
            Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(
                        stepsToday?.let { "~$it" } ?: "–", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1,
                    )
                }
                Text(stringResource(R.string.tile_steps), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
