package com.spcode.tnpscprep.ui.wrapup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.data.PlanDraftItem
import com.spcode.tnpscprep.data.PlanRefType
import com.spcode.tnpscprep.domain.DayRecap
import com.spcode.tnpscprep.domain.FoodState
import com.spcode.tnpscprep.domain.LogicalDay
import com.spcode.tnpscprep.domain.Mood
import com.spcode.tnpscprep.domain.WrapupHeadings
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.design.animatedInt
import com.spcode.tnpscprep.ui.design.rememberReduceMotion
import com.spcode.tnpscprep.ui.diary.DiaryMood
import com.spcode.tnpscprep.ui.localized
import com.spcode.tnpscprep.ui.today.ConfettiBurst
import com.spcode.tnpscprep.ui.today.TodayViewModel
import java.time.ZoneId

private const val STEPS = 6

/** Soft background tint that follows the chosen mood (subtle, never a verdict on how she feels). */
@Composable
internal fun moodTint(mood: String?): Color {
    val c = MaterialTheme.colorScheme
    return when (mood) {
        "LAUGH", "CHEER", "CELEBRATE" -> c.tertiary.copy(alpha = 0.16f)
        "PROUD" -> c.primary.copy(alpha = 0.12f)
        "WORRIED" -> c.secondary.copy(alpha = 0.12f)
        "CRY" -> c.onSurface.copy(alpha = 0.08f)
        "ANGRY" -> c.primary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
}

/** The Evening Wrap-up: a full-screen, six-step pager. Every step can be skipped; leaving midway keeps a draft. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrapupScreen(todayVm: TodayViewModel, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as TnpscApp
    val vm = viewModel<WrapupViewModel>(
        factory = viewModelFactory {
            initializer {
                val c = app.container
                WrapupViewModel(c.activityRepository, c.syllabusRepository, c.diaryRepository, c.plannedTaskRepository, c.settingsStore, c.moodRepository)
            }
        },
    )
    val s by vm.state.collectAsStateWithLifecycle()
    val ui by todayVm.ui.collectAsStateWithLifecycle()
    val extras by todayVm.extras.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val reduce = rememberReduceMotion()
    val day = remember { LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault()) }
    LaunchedEffect(Unit) { vm.load(day) }
    val status = viewModel<WrapupStatusViewModel>(
        factory = viewModelFactory { initializer { WrapupStatusViewModel(app.container.diaryRepository, app.container.plannedTaskRepository) } },
    )
    LaunchedEffect(Unit) { status.setDay(day) }
    val lastSeven by status.lastSeven.collectAsStateWithLifecycle()

    val headings = WrapupHeadings(stringResource(R.string.wrap_h_well), stringResource(R.string.wrap_h_hard), stringResource(R.string.wrap_h_tomorrow))
    var leaveAsk by rememberSaveable { mutableStateOf(false) }
    val finished = s.step >= STEPS - 1
    fun attemptLeave() { if (finished || !s.dirty) { if (!finished) vm.discardDraft(); onClose() } else leaveAsk = true }
    BackHandler(onBack = ::attemptLeave)

    // suggestions for tomorrow: due revisions, the next unread topic, a news item to review (existing data only)
    val rev = stringResource(R.string.wrap_sug_revise)
    val read = stringResource(R.string.wrap_sug_read)
    val news = stringResource(R.string.wrap_sug_news)
    val suggestions = buildList {
        ui.progress.due.take(3).forEach { d ->
            val n = localized(d.nameEn, d.nameTa)
            add(PlanDraftItem(String.format(if (d.affairCategory != null) news else rev, n), if (d.affairCategory != null) PlanRefType.CURRENT_AFFAIRS else PlanRefType.TOPIC, d.topicId))
        }
        extras.nextTopic?.let { add(PlanDraftItem(String.format(read, localized(it.nameEn, it.nameTa)), PlanRefType.TOPIC, it.id)) }
        extras.affairToReview?.let { add(PlanDraftItem(String.format(news, localized(it.titleEn, it.titleTa)), PlanRefType.CURRENT_AFFAIRS, it.id)) }
    }

    val tint by animateColorAsState(moodTint(s.mood), tween(if (reduce) 0 else 600), label = "moodTint")
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().background(tint))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = ::attemptLeave) { Icon(Icons.Rounded.Close, stringResource(R.string.sheet_close)) }
                Text(stringResource(R.string.wrap_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Dots(s.step)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (s.loaded) AnimatedContent(
                    targetState = s.step,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (fadeIn(tween(240, delayMillis = 60)) + slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { dir * it / 5 }) togetherWith
                            (fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { -dir * it / 5 })
                    },
                    label = "wrapStep",
                ) { step ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        when (step) {
                            0 -> RecapStep(s.recap, s.editing)
                            1 -> MoodStep(s.mood, s.intensity, vm::setMood, vm::setIntensity)
                            2 -> WriteStep(R.string.wrap_well_title, R.string.wrap_well_hint, listOf(R.string.wrap_well_c1, R.string.wrap_well_c2, R.string.wrap_well_c3, R.string.wrap_well_c4, R.string.wrap_well_c5), s.well, vm::setWell)
                            3 -> WriteStep(R.string.wrap_hard_title, R.string.wrap_hard_hint, listOf(R.string.wrap_hard_c1, R.string.wrap_hard_c2, R.string.wrap_hard_c3, R.string.wrap_hard_c4), s.hard, vm::setHard)
                            4 -> TomorrowStep(s.tomorrow, suggestions, vm::addTomorrow, vm::removeTomorrow, vm::moveTomorrow)
                            else -> FinishStep(s.savedSomething, s.mood, reduce, lastSeven)
                        }
                    }
                }
            }
            // bottom bar: Back / Skip / Next
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.step in 1..4) TextButton(onClick = { vm.go(s.step - 1) }) { Text(stringResource(R.string.wrap_back)) }
                Spacer(Modifier.weight(1f))
                when {
                    s.step == STEPS - 1 -> Button(onClick = onClose, modifier = Modifier.height(52.dp)) { Text(stringResource(R.string.wrap_done)) }
                    else -> {
                        if (s.step in 1..4) TextButton(onClick = {
                            if (s.step == 4) vm.finish(headings) { vm.go(5) } else vm.go(s.step + 1)
                        }) { Text(stringResource(R.string.wrap_skip)) }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (s.step == 4) vm.finish(headings) { haptic.performHapticFeedback(HapticFeedbackType.Confirm); vm.go(5) } else vm.go(s.step + 1)
                            },
                            modifier = Modifier.height(52.dp),
                        ) { Text(stringResource(if (s.step == 4) R.string.wrap_save else R.string.wrap_next)) }
                    }
                }
            }
        }
    }

    if (leaveAsk) {
        AlertDialog(
            onDismissRequest = { leaveAsk = false },
            title = { Text(stringResource(R.string.wrap_leave_title)) },
            text = { Text(stringResource(R.string.wrap_leave_body)) },
            confirmButton = { TextButton(onClick = { leaveAsk = false; onClose() }) { Text(stringResource(R.string.wrap_keep_draft)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { leaveAsk = false; vm.discardDraft(); onClose() }) { Text(stringResource(R.string.wrap_discard), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { leaveAsk = false }) { Text(stringResource(R.string.wrap_continue)) }
                }
            },
        )
    }
}

@Composable
private fun Dots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(STEPS) { i ->
            val w by androidx.compose.animation.core.animateDpAsState(if (i == step) 20.dp else 7.dp, label = "dot")
            Box(Modifier.size(w, 7.dp).clip(CircleShape).background(if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

private val diaryFont = FontFamily.Serif

@Composable
private fun H(text: String) = Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontFamily = diaryFont))

@Composable
private fun CountUp(value: Int, style: androidx.compose.ui.text.TextStyle, reduce: Boolean) {
    Text((if (reduce) value else animatedInt(value)).toString(), style = style.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
}

// ---------------------------------------------------------------- step 1: recap

@Composable
private fun RecapStep(r: DayRecap?, editing: Boolean) {
    val reduce = rememberReduceMotion()
    H(stringResource(R.string.wrap_recap_title))
    if (editing) Text(stringResource(R.string.wrap_editing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (r == null) return
    if (r.isEmpty) {
        Text(stringResource(R.string.wrap_recap_empty), style = MaterialTheme.typography.bodyLarge.copy(fontFamily = diaryFont, lineHeight = 28.sp))
        return
    }
    if (r.studyMinutes > 0 || r.focusSessions > 0) SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CountUp(r.studyMinutes, MaterialTheme.typography.displaySmall, reduce)
                Text(stringResource(R.string.wrap_r_min_goal, r.goalMinutes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(stringResource(R.string.wrap_r_sessions, r.focusSessions, r.longestSessionMinutes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (r.topicsAdvancedCount > 0 || r.affairsRevised > 0) SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (r.topicsAdvancedCount > 0) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CountUp(r.topicsAdvancedCount, MaterialTheme.typography.headlineMedium, reduce)
                    Text(stringResource(R.string.wrap_r_topics), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 3.dp))
                }
                if (r.topicsAdvanced.isNotEmpty()) Text(r.topicsAdvanced.take(4).joinToString(" · ") + if (r.topicsAdvanced.size > 4) " …" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (r.affairsRevised > 0) Text(stringResource(R.string.wrap_r_affairs, r.affairsRevised), style = MaterialTheme.typography.bodyMedium)
        }
    }
    val bits = buildList {
        r.food?.let { f -> add(stringResource(R.string.ev_food, stringResource(when (f) { FoodState.HEALTHY -> R.string.food_healthy; FoodState.JUNK -> R.string.food_junk; FoodState.MIXED -> R.string.food_mixed }))) }
        r.steps?.let { add(stringResource(R.string.wrap_r_steps, it)) }
        if (r.diaryEntries > 0) add(stringResource(R.string.wrap_r_diary, r.diaryEntries))
    }
    if (bits.isNotEmpty()) SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { bits.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) } }
    }
}

// ---------------------------------------------------------------- step 2: mood + intensity

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodStep(mood: String?, intensity: Int, onMood: (String?) -> Unit, onIntensity: (Int) -> Unit) {
    H(stringResource(R.string.wrap_mood_title))
    Text(stringResource(R.string.wrap_mood_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Mood.entries.forEach { m ->
            val sel = mood == m.name
            Box(
                Modifier.size(60.dp).clip(CircleShape)
                    .border(BorderStroke(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), CircleShape)
                    .clickable { onMood(if (sel) null else m.name) },
                contentAlignment = Alignment.Center,
            ) { DiaryMood(m.name, 52.dp) }
        }
    }
    if (mood != null) {
        Text(stringResource(R.string.wrap_intensity), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.wrap_int_light), style = MaterialTheme.typography.labelMedium)
            (1..5).forEach { i ->
                val on = intensity >= i
                val desc = i.toString()
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(BorderStroke(1.5.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), CircleShape)
                        .semantics { contentDescription = desc }.clickable { onIntensity(i) },
                )
            }
            Text(stringResource(R.string.wrap_int_strong), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ---------------------------------------------------------------- steps 3 + 4: free text with quick picks

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WriteStep(title: Int, hint: Int, chips: List<Int>, text: String, onText: (String) -> Unit) {
    H(stringResource(title))
    val labels = chips.map { stringResource(it) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEach { l ->
            FilterChip(selected = text.contains(l), onClick = { if (!text.contains(l)) onText(if (text.isBlank()) l else text.trimEnd() + "\n" + l) }, label = { Text(l) })
        }
    }
    val style = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = diaryFont, fontSize = 19.sp, lineHeight = 32.sp, color = MaterialTheme.colorScheme.onSurface,
        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None),
    )
    BasicTextField(
        value = text, onValueChange = onText, textStyle = style, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().heightMin(180),
        decorationBox = { inner ->
            Box { if (text.isEmpty()) Text(stringResource(hint), style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))); inner() }
        },
    )
}

private fun Modifier.heightMin(dp: Int): Modifier = this.then(Modifier.defaultMinSize(minHeight = dp.dp))

// ---------------------------------------------------------------- step 5: tomorrow's top 3

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TomorrowStep(
    items: List<PlanDraftItem>, suggestions: List<PlanDraftItem>,
    onAdd: (PlanDraftItem) -> Unit, onRemove: (Int) -> Unit, onMove: (Int, Int) -> Unit,
) {
    H(stringResource(R.string.wrap_tomorrow_title))
    Text(stringResource(R.string.wrap_tomorrow_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    items.forEachIndexed { i, it ->
        SoftCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
                Text(it.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { onMove(i, i - 1) }, enabled = i > 0) { Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.wrap_up)) }
                IconButton(onClick = { onMove(i, i + 1) }, enabled = i < items.lastIndex) { Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.wrap_down)) }
                IconButton(onClick = { onRemove(i) }) { Icon(Icons.Rounded.Close, stringResource(R.string.wrap_remove)) }
            }
        }
    }
    val room = items.size < WrapupViewModel.MAX_TOMORROW
    val open = suggestions.filter { s -> items.none { it.title == s.title || (s.refId != null && it.refId == s.refId && it.refType == s.refType) } }
    if (room && open.isNotEmpty()) {
        Text(stringResource(R.string.wrap_suggest), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            open.forEach { sug -> FilterChip(selected = false, onClick = { onAdd(sug) }, label = { Text(sug.title, maxLines = 2) }) }
        }
    }
    if (room) {
        var custom by rememberSaveable { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedTextField(
                value = custom, onValueChange = { custom = it.take(120) }, singleLine = true, modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.wrap_custom_hint)) },
            )
            OutlinedButton(onClick = { onAdd(PlanDraftItem(custom, PlanRefType.CUSTOM, null)); custom = "" }, enabled = custom.isNotBlank()) { Text(stringResource(R.string.wrap_add)) }
        }
    }
}

// ---------------------------------------------------------------- step 6: finish

@Composable
private fun FinishStep(wrote: Boolean, mood: String?, reduce: Boolean, lastSeven: Int) {
    Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
        if (wrote && !reduce) ConfettiBurst(Modifier.fillMaxSize(), originX = 0.5f)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // the mood she chose plays its own reaction (confetti, tears, steam, sparkle...); the default is a calm thumbs-up
            val m = mood?.let { n -> Mood.entries.firstOrNull { it.name == n } }
            if (m != null && wrote) com.spcode.tnpscprep.ui.mood.MoodAvatar(m, size = 140.dp) else DiaryMood(if (wrote) "PROUD" else "CHEER", 120.dp)
            Text(
                stringResource(if (wrote) R.string.wrap_finish_written else R.string.wrap_finish_empty),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = diaryFont, lineHeight = 32.sp), textAlign = TextAlign.Center,
            )
            if (lastSeven >= 1) Text(stringResource(R.string.wrap_consistency, lastSeven), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
