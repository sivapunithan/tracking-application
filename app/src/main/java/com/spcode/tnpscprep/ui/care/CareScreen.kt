package com.spcode.tnpscprep.ui.care

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.spcode.tnpscprep.ui.guide.guideTarget
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.data.HealthKind
import com.spcode.tnpscprep.domain.CareStats
import com.spcode.tnpscprep.domain.FoodCheckin
import com.spcode.tnpscprep.domain.Mood
import com.spcode.tnpscprep.domain.MoodCheckins
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.PopIn
import com.spcode.tnpscprep.ui.design.PopSheet
import com.spcode.tnpscprep.ui.design.ProgressRing
import com.spcode.tnpscprep.ui.design.ScreenTitle
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.design.animatedInt
import com.spcode.tnpscprep.ui.diary.DiaryMood
import com.spcode.tnpscprep.ui.mood.MoodAvatar
import com.spcode.tnpscprep.ui.theme.LocalIsDark
import com.spcode.tnpscprep.ui.today.FoodEmoji
import com.spcode.tnpscprep.ui.today.FoodPickSheet
import com.spcode.tnpscprep.ui.today.FoodViewModel
import com.spcode.tnpscprep.ui.today.HealthSheet
import com.spcode.tnpscprep.ui.today.HealthViewModel
import com.spcode.tnpscprep.ui.today.MoodPickSheet
import com.spcode.tnpscprep.ui.today.MoodViewModel
import com.spcode.tnpscprep.ui.today.SectionTitle
import com.spcode.tnpscprep.ui.today.StepsCard
import com.spcode.tnpscprep.ui.today.StepsViewModel
import com.spcode.tnpscprep.ui.today.foodColor
import com.spcode.tnpscprep.ui.today.hasStepSensor
import com.spcode.tnpscprep.ui.today.moodLabel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * The Care tab: one calm page for how she is doing. A hero card (today's mood picture plays its own reaction, food / water / steps at a glance),
 * then Mood, Food, Water, Steps, and the private body and cycle log behind a lock. Counts and logs only: no scores, no diagnosis.
 */
@Composable
fun CareScreen() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TnpscApp
    val c = app.container
    val hvm = viewModel<HealthViewModel>(factory = viewModelFactory { initializer { HealthViewModel(c.healthRepository, c.settingsStore) } })
    val fvm = viewModel<FoodViewModel>(factory = viewModelFactory { initializer { FoodViewModel(c.foodRepository, c.settingsStore) } })
    val mvm = viewModel<MoodViewModel>(factory = viewModelFactory { initializer { MoodViewModel(c.moodRepository) } })
    val svm = viewModel<StepsViewModel>(factory = viewModelFactory { initializer { StepsViewModel(c.stepRepository) } })
    val stepSensor = remember { hasStepSensor(app) }
    val food by fvm.days.collectAsStateWithLifecycle()
    val moods by mvm.days.collectAsStateWithLifecycle()
    val health by hvm.entries.collectAsStateWithLifecycle()
    val steps by svm.week.collectAsStateWithLifecycle()
    val today = remember { FoodCheckin.foodDay(System.currentTimeMillis(), java.time.ZoneId.systemDefault()) }
    var sheet by rememberSaveable { mutableStateOf<String?>(null) } // "mood:<date>" / "food:<date>"
    var bodyOpen by rememberSaveable { mutableStateOf(false) }

    val waterRows = health.filter { it.kind == HealthKind.WATER }.mapNotNull { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { it to e.value.toInt() } }
    val waterWeek = CareStats.waterDays(waterRows, today)
    val weight = health.filter { it.kind == HealthKind.WEIGHT }.maxByOrNull { it.createdAt }?.value
    val target = CareStats.waterTarget(weight)
    val waterToday = waterWeek.last().second
    val lastGlass = health.filter { it.kind == HealthKind.WATER && it.date == today.toString() }.maxByOrNull { it.createdAt }
    val stepsToday = if (stepSensor) steps.firstOrNull { it.date == today.toString() }?.steps else null

    val cb = com.spcode.tnpscprep.ui.guide.rememberGuideBounds()
    val careScroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(careScroll).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(R.string.care_title))

        // ---------------- hero ----------------
        PopIn(0, Modifier.guideTarget(cb, "hero")) { Hero(moods[today], food[today], waterToday, target, stepsToday) { sheet = "mood:$today" } }

        // ---------------- mood ----------------
        PopIn(1, Modifier.guideTarget(cb, "mood")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.care_mood))
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MoodCheckins.lastDays(today, 7).forEach { d ->
                                val m = moods[d]
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { sheet = "mood:$d" }.padding(2.dp)) {
                                    Box(Modifier.size(38.dp).clip(CircleShape).border(BorderStroke(if (d == today) 2.dp else 1.dp, if (d == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), CircleShape), contentAlignment = Alignment.Center) {
                                        if (m != null) DiaryMood(m.name, 34.dp) else Text("+", color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        val counts = MoodCheckins.counts(moods, today, 30)
                        if (counts.isEmpty()) {
                            Text(stringResource(R.string.care_mood_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(stringResource(R.string.care_mood_30), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                counts.forEach { (m, n) -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { DiaryMood(m.name, 28.dp); Text("×$n", style = MaterialTheme.typography.labelLarge) } }
                            }
                        }
                    }
                }
            }
        }

        // ---------------- food ----------------
        PopIn(2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.care_food))
                FoodHistoryCard(food, today, onDay = { sheet = "food:$it" })
            }
        }

        // ---------------- water ----------------
        PopIn(3, Modifier.guideTarget(cb, "water")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(stringResource(R.string.checkin_water))
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(CareStats.fraction(waterToday, target), Modifier.size(72.dp), strokeWidth = 8.dp, track = MaterialTheme.colorScheme.outlineVariant, color = MaterialTheme.colorScheme.primary) {
                                Text(animatedInt(waterToday).toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.water_of, target), style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(if (weight != null) R.string.water_target_weight else R.string.water_target_default), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // 7-day bars (ml per day), today highlighted
                        val maxMl = maxOf(waterWeek.maxOf { it.second }, target, 1)
                        Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                            waterWeek.forEach { (d, ml) ->
                                Box(Modifier.weight(1f).height((4 + 52f * ml / maxMl).dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = if (d == today) 1f else if (ml > 0) 0.45f else 0.15f)))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            waterWeek.forEach { (d, _) -> Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { hvm.add(HealthKind.WATER, 250.0) }) { Text("+250") }
                            OutlinedButton(onClick = { hvm.add(HealthKind.WATER, 500.0) }) { Text("+500") }
                            Spacer(Modifier.weight(1f))
                            if (lastGlass != null) OutlinedButton(onClick = { hvm.delete(lastGlass.id) }) { Icon(Icons.Rounded.Remove, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.care_water_undo)) }
                        }
                    }
                }
            }
        }

        // ---------------- steps ----------------
        if (stepSensor) PopIn(4) { StepsCard() }

        // ---------------- private body + cycle ----------------
        PopIn(5, Modifier.guideTarget(cb, "body")) {
            SoftCard(Modifier.fillMaxWidth(), onClick = { bodyOpen = !bodyOpen }) {
                Column(Modifier.padding(16.dp).animateContentSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.care_private_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(stringResource(R.string.care_private_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (bodyOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    }
                    if (bodyOpen) { Spacer(Modifier.height(12.dp)); HealthSheet(hvm, dismiss = null, showTitle = false) }
                }
            }
        }
    }
    com.spcode.tnpscprep.ui.guide.PageTour(
        "care",
        listOf(
            com.spcode.tnpscprep.ui.guide.CoachStep("hero", stringResource(R.string.guide_c1_title), stringResource(R.string.guide_c1_body)),
            com.spcode.tnpscprep.ui.guide.CoachStep("mood", stringResource(R.string.guide_c2_title), stringResource(R.string.guide_c2_body)),
            com.spcode.tnpscprep.ui.guide.CoachStep("water", stringResource(R.string.guide_c3_title), stringResource(R.string.guide_c3_body)),
            com.spcode.tnpscprep.ui.guide.CoachStep("body", stringResource(R.string.guide_c4_title), stringResource(R.string.guide_c4_body)),
        ),
        cb, scrollable = careScroll, enabled = sheet == null,
    )
    }

    // ---------------- pick sheets (mood / food for any day) ----------------
    val s = sheet
    if (s != null) {
        val day = runCatching { LocalDate.parse(s.substringAfter(':')) }.getOrNull()
        if (day == null) sheet = null else PopSheet(onDismiss = { sheet = null }) { dismiss ->
            if (s.startsWith("mood:")) MoodPickSheet(moods[day]) { mvm.set(day, it); dismiss() }
            else FoodPickSheet(food[day]) { fvm.set(day, it); dismiss() }
        }
    }
}

/** Crimson hero (like the Next best step card): today's mood picture, big and animated, and food / water / steps at a glance. */
@Composable
private fun Hero(mood: Mood?, food: com.spcode.tnpscprep.domain.FoodState?, waterMl: Int, target: Int, steps: Int?, onMood: () -> Unit) {
    val dark = LocalIsDark.current
    val brush = Brush.linearGradient(if (dark) listOf(Color(0xFF9B0F26), Color(0xFF4A0A14)) else listOf(Color(0xFFC8102E), Color(0xFF7A0019)))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(brush).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.care_hero_title), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                Text(
                    mood?.let { moodLabel(it) } ?: stringResource(R.string.care_hero_none), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White,
                )
                Text(stringResource(R.string.care_hero_tap), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Box(Modifier.size(104.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable(onClick = onMood), contentAlignment = Alignment.Center) {
                if (mood != null) MoodAvatar(mood, size = 100.dp) else Text("+", style = MaterialTheme.typography.displaySmall, color = Color.White)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Glance(Modifier.weight(1f), stringResource(R.string.checkin_food)) {
                if (food != null) Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { FoodEmoji(food.name.lowercase(), 30.dp) } else Text("–", color = Color.White)
            }
            Glance(Modifier.weight(1f), stringResource(R.string.checkin_water)) {
                Text("$waterMl", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("/$target", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            }
            if (steps != null) Glance(Modifier.weight(1f), stringResource(R.string.care_steps_short)) { Text("~$steps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White) }
        }
    }
}

@Composable
private fun Glance(modifier: Modifier, label: String, content: @Composable () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.14f)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) { content() }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}
