package com.spcode.tnpscprep.ui.today

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.MoodEngine
import com.spcode.tnpscprep.domain.MoodInput
import com.spcode.tnpscprep.domain.RevisionLevel
import com.spcode.tnpscprep.domain.RevisionSchedule
import com.spcode.tnpscprep.ui.affairs.categoryLabelRes
import com.spcode.tnpscprep.ui.design.AnimatedProgressBar
import com.spcode.tnpscprep.ui.design.LevelTrack
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.PopIn
import com.spcode.tnpscprep.ui.design.PopSheet
import com.spcode.tnpscprep.ui.design.ProgressRing
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.design.animatedInt
import com.spcode.tnpscprep.ui.design.pressScale
import com.spcode.tnpscprep.ui.localized
import com.spcode.tnpscprep.ui.mood.MoodAvatar
import com.spcode.tnpscprep.ui.mood.hasMoodImage
import com.spcode.tnpscprep.ui.syllabus.ExamFilter
import com.spcode.tnpscprep.ui.theme.LocalIsDark
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(vm: TodayViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val name by vm.userName.collectAsStateWithLifecycle()
    val heat by vm.heat.collectAsStateWithLifecycle()
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val stepSensor = remember { hasStepSensor(appCtx) }
    // Which pop-up is open: null, "date", "due:<id>" or "star:<id>" (saveable, so rotation keeps it).
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(name, ui) }

        item { HeroCard(ui, onPickDate = { sheet = "date" }) }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExamFilter.entries.forEach { f ->
                    FilterChip(
                        selected = f == ui.target,
                        onClick = { vm.setTarget(f) },
                        label = { Text(if (f == ExamFilter.ALL) stringResource(R.string.filter_all) else f.name) },
                    )
                }
            }
        }

        item { StatsRow(ui) }

        item { HeatmapSection(heat, onDay = { sheet = "day:" + it }) }

        if (stepSensor) item { StepsCard() }

        item {
            val studyByDate = remember(heat) { heat.weeks.flatten().filterNotNull().associate { it.date to it.minutes } }
            UsageCard(studyByDate)
        }

        item {
            SoftCard(Modifier.fillMaxWidth(), onClick = { sheet = "health" }) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.health_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.health_card_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // --- due for revision ---
        item { SectionTitle(stringResource(R.string.due_title, ui.progress.due.size)) }
        if (ui.progress.due.isEmpty()) {
            item { EmptyHint(stringResource(R.string.due_empty)) }
        } else {
            itemsIndexed(ui.progress.due, key = { _, d -> "due-" + d.topicId }) { i, d ->
                PopIn(i) {
                    val levelName = stringArrayResource(R.array.revision_levels)[RevisionLevel.clamp(d.level)]
                    SoftCard(Modifier.fillMaxWidth(), onClick = { sheet = "due:" + d.topicId }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(localized(d.nameEn, d.nameTa), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                Text(
                                    levelName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                if (d.daysOverdue == 0) stringResource(R.string.due_today)
                                else pluralStringResource(R.plurals.overdue_days, d.daysOverdue, d.daysOverdue),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (d.daysOverdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // --- progress by subject (one card, animated bars) ---
        if (ui.progress.subjects.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.subject_progress_title)) }
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ui.progress.subjects.forEach { sp ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(localized(sp.nameEn, sp.nameTa), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        stringResource(R.string.percent_format, animatedInt(sp.percent)),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                AnimatedProgressBar(sp.percent / 100f, height = 8.dp)
                            }
                        }
                    }
                }
            }
        }

        // --- starred (weak) topics ---
        item { SectionTitle(stringResource(R.string.starred_title, ui.progress.starred.size)) }
        if (ui.progress.starred.isEmpty()) {
            item { EmptyHint(stringResource(R.string.starred_empty)) }
        } else {
            itemsIndexed(ui.progress.starred, key = { _, s -> "star-" + s.topicId }) { i, st ->
                PopIn(i) {
                    SoftCard(Modifier.fillMaxWidth(), onClick = { sheet = "star:" + st.topicId }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(localized(st.nameEn, st.nameTa), style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                Text(
                                    localized(st.subjectEn, st.subjectTa),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- pop-ups ----------------
    val current = sheet
    if (current != null) {
        PopSheet(onDismiss = { sheet = null }) { dismiss ->
            when {
                current == "date" -> ExamDateSheet(ui, onSave = { vm.setExamDate(it); dismiss() }, onClear = { vm.clearExamDate(); dismiss() }, onCancel = dismiss)
                current == "health" -> {
                    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp
                    val hvm = androidx.lifecycle.viewmodel.compose.viewModel<HealthViewModel>(
                        factory = androidx.lifecycle.viewmodel.viewModelFactory {
                            initializer { HealthViewModel(app.container.healthRepository, app.container.settingsStore) }
                        },
                    )
                    HealthSheet(hvm, dismiss)
                }
                current.startsWith("day:") -> {
                    val date = runCatching { java.time.LocalDate.parse(current.removePrefix("day:")) }.getOrNull()
                    if (date == null) LaunchedEffect(Unit) { dismiss() } else DaySheet(vm, date, dismiss)
                }
                current.startsWith("due:") -> {
                    val item = ui.progress.due.firstOrNull { it.topicId == current.removePrefix("due:") }
                    if (item == null) LaunchedEffect(Unit) { dismiss() } else DueSheet(item, onRevised = { vm.markRevised(item) }, dismiss = dismiss)
                }
                current.startsWith("star:") -> {
                    val item = ui.progress.starred.firstOrNull { it.topicId == current.removePrefix("star:") }
                    if (item == null) LaunchedEffect(Unit) { dismiss() } else StarSheet(item, onUnstar = { vm.toggleStar(item.topicId); dismiss() }, dismiss = dismiss)
                }
            }
        }
    }
}

@Composable
private fun Header(name: String, ui: TodayUiState) {
    val hour = remember { LocalTime.now().hour }
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greet_morning
            hour < 17 -> R.string.greet_afternoon
            else -> R.string.greet_evening
        },
    )
    // The avatar pops in with a springy overshoot.
    val avatarScale by animateFloatAsState(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "avatar")
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); started = true }
    val scale by animateFloatAsState(if (started) avatarScale else 0.5f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "avatarIn")

    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(68.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))),
        ) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val mood = remember(ui.streak, ui.todayMinutes, ui.goalMinutes, ui.daysSinceStudy, ui.progress.due.size, hour) {
                MoodEngine.pick(MoodInput(ui.streak, ui.todayMinutes >= ui.goalMinutes, ui.daysSinceStudy, ui.progress.due.size, hour))
            }
            // The avatar follows her streak and revision, but only once she has studied something and only when THAT mood's own
            // picture exists; otherwise (new user, or picture not made yet) a plain initial badge.
            if (ui.daysSinceStudy != null && hasMoodImage(ctx, mood)) {
                MoodAvatar(mood, size = 68.dp)
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (name.isBlank()) {
                    Icon(Icons.Rounded.School, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
                } else {
                    Text(
                        name.first().uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (name.isBlank()) stringResource(R.string.welcome_back) else stringResource(R.string.hello_name, name),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                stringResource(R.string.today_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroCard(ui: TodayUiState, onPickDate: () -> Unit) {
    val dark = LocalIsDark.current
    val brush = Brush.linearGradient(
        if (dark) listOf(Color(0xFF9B0F26), Color(0xFF4A0A14)) else listOf(Color(0xFFC8102E), Color(0xFF7A0019)),
    )
    val days = ui.daysToExam
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(brush).padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(ui.progress.readinessPercent / 100f, Modifier.size(116.dp), strokeWidth = 12.dp) {
                Text(
                    stringResource(R.string.percent_format, animatedInt(ui.progress.readinessPercent)),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.readiness_title), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.92f))
                Text(
                    when {
                        ui.examDate == null -> stringResource(R.string.exam_date_not_set)
                        days != null && days >= 0 -> pluralStringResource(R.plurals.exam_in_days, days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), days.toInt())
                        else -> stringResource(R.string.exam_date_passed)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    stringResource(R.string.not_started_count, ui.progress.notStarted, ui.progress.totalTopics),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f))
                        .pressScale(onClick = onPickDate).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.set_exam_date), style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(ui: TodayUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        SoftCard(Modifier.weight(1f)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(
                    (ui.todayMinutes.toFloat() / ui.goalMinutes).coerceIn(0f, 1f), Modifier.size(54.dp), strokeWidth = 7.dp,
                    track = MaterialTheme.colorScheme.outlineVariant, color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(animatedInt(ui.todayMinutes).toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(
                        stringResource(R.string.stat_minutes_of, ui.goalMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SoftCard(Modifier.weight(1f)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocalFireDepartment, null,
                    tint = if (ui.streak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(animatedInt(ui.streak).toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(
                        stringResource(R.string.stat_streak_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------- pop-up contents

@Composable
private fun DueSheet(item: DueItem, onRevised: () -> Unit, dismiss: () -> Unit) {
    var done by remember { mutableStateOf(false) }
    val levelNames = stringArrayResource(R.array.revision_levels)
    val nextLevel = (item.level + 1).coerceAtMost(RevisionLevel.MAX)
    val nextDays = RevisionSchedule.intervalDays(nextLevel) ?: 0
    val tick by animateFloatAsState(if (done) 1f else 0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium), label = "tick")
    LaunchedEffect(done) { if (done) { delay(750); dismiss() } }

    if (done) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(96.dp).graphicsLayer { scaleX = tick; scaleY = tick },
            )
        }
        return
    }
    Text(localized(item.nameEn, item.nameTa), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    Text(
        if (item.affairCategory != null) stringResource(R.string.tab_affairs) + "  ·  " + stringResource(categoryLabelRes(item.affairCategory))
        else localized(item.subjectEn, item.subjectTa),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(20.dp))
    LevelTrack(item.level, Modifier.fillMaxWidth())
    Text(
        levelNames[RevisionLevel.clamp(item.level)],
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp),
    )
    Text(
        if (item.daysOverdue == 0) stringResource(R.string.due_today)
        else pluralStringResource(R.plurals.overdue_days, item.daysOverdue, item.daysOverdue),
        style = MaterialTheme.typography.bodyMedium,
        color = if (item.daysOverdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
    )
    Text(
        pluralStringResource(R.plurals.sheet_next_review, nextDays, nextDays),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = { onRevised(); done = true }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Text(stringResource(R.string.revised), style = MaterialTheme.typography.titleMedium)
    }
    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
}

@Composable
private fun StarSheet(item: StarredItem, onUnstar: () -> Unit, dismiss: () -> Unit) {
    val levelNames = stringArrayResource(R.array.revision_levels)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(localized(item.nameEn, item.nameTa), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
    }
    Text(
        localized(item.subjectEn, item.subjectTa),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(18.dp))
    LevelTrack(item.level, Modifier.fillMaxWidth())
    Text(
        levelNames[RevisionLevel.clamp(item.level)],
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp),
    )
    Spacer(Modifier.height(16.dp))
    SoftCard(Modifier.fillMaxWidth()) {
        Text(
            if (item.note.isBlank()) stringResource(R.string.sheet_no_note) else item.note,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp),
        )
    }
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = onUnstar, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.sheet_remove_star)) }
    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDateSheet(ui: TodayUiState, onSave: (Long) -> Unit, onClear: () -> Unit, onCancel: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = ui.examDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    Text(stringResource(R.string.set_exam_date), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    ui.examDate?.let {
        Text(
            it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    DatePicker(state = state, showModeToggle = false, title = null, headline = null)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (ui.examDate != null) OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.clear)) }
        Button(
            onClick = {
                // The picker works in UTC midnight; convert back to a plain calendar date.
                state.selectedDateMillis?.let { onSave(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) } ?: onCancel()
            },
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text(stringResource(R.string.ok)) }
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
}
