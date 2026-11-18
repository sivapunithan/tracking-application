package com.spcode.tnpscprep.ui.diary

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.spcode.tnpscprep.ui.design.SoftCard
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.data.db.DiaryEntryEntity
import com.spcode.tnpscprep.domain.DiaryItem
import com.spcode.tnpscprep.domain.DiaryLimits
import com.spcode.tnpscprep.domain.DiaryPrompts
import com.spcode.tnpscprep.domain.DiaryMemories
import com.spcode.tnpscprep.domain.DiaryReview
import com.spcode.tnpscprep.domain.DiaryTimeline
import com.spcode.tnpscprep.domain.LogicalDay
import com.spcode.tnpscprep.domain.Mood
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.mood.moodImageRes
import com.spcode.tnpscprep.ui.theme.LocalIsDark
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun paper(): Color = if (LocalIsDark.current) MaterialTheme.colorScheme.background else Color(0xFFFBF6EC)

/** The Journal tab root: diary timeline + search + weekly review. Entries open as a full-screen page (the editor route). */
@Composable
fun JournalScreen(
    vm: DiaryViewModel, todayVm: com.spcode.tnpscprep.ui.today.TodayViewModel, studyByDate: Map<LocalDate, Int>,
    onOpen: (Long) -> Unit, onNew: () -> Unit, onNewOn: (String) -> Unit, onWrapup: () -> Unit,
) {
    var selectedText by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val app = LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp
    val fvm = androidx.lifecycle.viewmodel.compose.viewModel<com.spcode.tnpscprep.ui.today.FoodViewModel>(
        factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { com.spcode.tnpscprep.ui.today.FoodViewModel(app.container.foodRepository, app.container.settingsStore) } },
    )
    val food by fvm.days.collectAsStateWithLifecycle()
    val wvm = androidx.lifecycle.viewmodel.compose.viewModel<com.spcode.tnpscprep.ui.wrapup.WrapupStatusViewModel>(
        factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { com.spcode.tnpscprep.ui.wrapup.WrapupStatusViewModel(app.container.diaryRepository, app.container.plannedTaskRepository) } },
    )
    LaunchedEffect(Unit) { wvm.setDay(LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault())) }
    val wrappedToday by wvm.wrappedToday.collectAsStateWithLifecycle()
    val lastSeven by wvm.lastSeven.collectAsStateWithLifecycle()
    val year by todayVm.yearHeat.collectAsStateWithLifecycle()
    val diaryItems by vm.items.collectAsStateWithLifecycle()
    val diaryDates = remember(diaryItems) { diaryItems.map { it.date }.toSet() }
    val today = remember { LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault()) }
    val selected = selectedText?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    val moods = remember(diaryItems) { diaryItems.filter { it.mood != null }.groupBy { it.date }.mapValues { it.value.first().mood!! } }
    val undo by vm.undo.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    LaunchedEffect(undo) { if (undo != null) { delay(5_000); vm.clearUndo() } }
    LaunchedEffect(notice) { if (notice != null) { delay(4_000); vm.clearNotice() } }
    val deletedText = stringResource(R.string.diary_deleted)
    Surface(Modifier.fillMaxSize(), color = paper()) {
      Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            TimelinePane(
                vm, studyByDate, onOpen = onOpen, onNew = onNew, onNewOn = onNewOn, wrapped = wrappedToday, lastSeven = lastSeven, onWrapup = onWrapup,
                weeks = year.weeks, food = food, moods = moods, today = today, selected = selected, onSelect = { selectedText = it.toString() },
            )
            Column(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = LocalBottomBarInset.current)) {
                AnimatedVisibility(undo != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                    val u = undo
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.inverseSurface, tonalElevation = 6.dp) {
                        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(deletedText, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.weight(1f))
                            TextButton(onClick = { u?.let(vm::restore); vm.clearUndo() }) { Text(stringResource(R.string.diary_undo), color = MaterialTheme.colorScheme.inversePrimary) }
                        }
                    }
                }
                AnimatedVisibility(notice != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.inverseSurface, tonalElevation = 6.dp) {
                        Text(notice.orEmpty(), color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
      }
    }
}

/** One diary entry as a full-screen page. [entryId] = open that entry; otherwise a new entry for [dateArg] (yyyy-MM-dd) or today's logical day. */
@Composable
fun JournalEditorScreen(vm: DiaryViewModel, entryId: Long?, dateArg: String?, onDone: (removed: DiaryEntryEntity?) -> Unit, onWrapup: () -> Unit = {}) {
    val date = remember(dateArg) {
        dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault())
    }
    Surface(Modifier.fillMaxSize(), color = paper()) { EditorPane(vm, entryId, date, onDone, onWrapup) }
}

// ---------------------------------------------------------------- mood emoji (reuses the mood avatars)

@Composable
fun DiaryMood(id: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val res = remember(id, size) { Mood.entries.firstOrNull { it.name == id }?.let { moodImageRes(ctx, it, size) } ?: 0 }
    val bitmap = if (res != 0) remember(res) { com.spcode.tnpscprep.ui.mood.MoodBitmaps.get(ctx, res) } else null
    if (bitmap != null) {
        Image(bitmap, null, modifier.size(size), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
    } else if (id != null) {
        // picture not on this phone: a small badge with the mood's first letter
        Box(modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(id.take(1), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

// ---------------------------------------------------------------- timeline

@Composable
private fun TimelinePane(
    vm: DiaryViewModel, studyByDate: Map<LocalDate, Int>, onOpen: (Long) -> Unit, onNew: () -> Unit, onNewOn: (String) -> Unit, wrapped: Boolean, lastSeven: Int, onWrapup: () -> Unit,
    weeks: List<List<com.spcode.tnpscprep.domain.DaySummary?>>, food: Map<LocalDate, com.spcode.tnpscprep.domain.FoodState>, moods: Map<LocalDate, String>,
    today: LocalDate, selected: LocalDate, onSelect: (LocalDate) -> Unit,
) {
    val onNotice: (String) -> Unit = vm::postNotice
    val ctx = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val pinned = remember(items, query) { if (query.isBlank()) items.filter { it.pinned } else emptyList() }
    val groups = remember(items, pinned) { DiaryTimeline.groupByMonth(items.filter { it !in pinned }) }
    val streak = remember(items) { com.spcode.tnpscprep.domain.DiaryStats.streak(items.map { it.date }.toSet(), LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault())) }
    val week = remember(items, studyByDate) { DiaryReview.week(items, today, studyByDate) }
    val scope = rememberCoroutineScope()
    val exportDone = stringResource(R.string.diary_export_done)
    val exportFail = stringResource(R.string.diary_export_failed)
    val importBad = stringResource(R.string.diary_import_bad)
    val importDone = stringResource(R.string.diary_import_done)

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { onNotice(if (vm.exportTo(ctx.contentResolver, uri)) exportDone else exportFail) }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            vm.importFrom(ctx.contentResolver, uri).fold(
                { o -> onNotice(String.format(importDone, o.added, o.updated, o.unchanged)) },
                { onNotice(importBad) },
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp + LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(stringResource(R.string.nav_journal), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
            }
            item(key = "wrapup-btn") {
                com.spcode.tnpscprep.ui.wrapup.WrapupCardView(if (wrapped) com.spcode.tnpscprep.domain.WrapupCardState.DONE else com.spcode.tnpscprep.domain.WrapupCardState.INVITE, onWrapup, lastSeven)
            }
            if (query.isBlank()) {
                val memories = DiaryMemories.onThisDay(items, today)
                if (memories.isNotEmpty()) item(key = "memories") { MemoryCard(memories, onOpen) }
                item(key = "calendar") { JournalCalendarCard(weeks, food, moods, items.map { it.date }.toSet(), today, selected, onSelect) }
                item(key = "daystory") {
                    DayStory(selected, today, items.filter { it.date == selected }, onOpen = onOpen, onWrite = { onNewOn(it.toString()) }, onWrapup = onWrapup, wrapped = wrapped)
                }
            }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { vm.query.value = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.diary_search_hint)) }, shape = RoundedCornerShape(16.dp),
                )
            }
            if (query.isBlank() && items.isNotEmpty()) item { ReviewCard(week, streak) }
            if (pinned.isNotEmpty()) {
                item(key = "pinned-h") {
                    Text(
                        stringResource(R.string.diary_pinned), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(pinned, key = { "p-" + it.id }) { e -> EntryCard(e, onClick = { onOpen(e.id) }, onPin = { vm.setPinned(e.id, false) }) }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(if (query.isBlank()) R.string.diary_empty else R.string.diary_no_results),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            if (items.isNotEmpty() && query.isBlank()) item(key = "all-h") {
                Text(stringResource(R.string.cal_all_entries), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
            }
            groups.forEach { g ->
                item(key = "m-" + g.month) {
                    Text(
                        g.month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                itemsIndexed(g.items, key = { _, it -> it.id }) { i, e -> com.spcode.tnpscprep.ui.design.PopIn(i) { EntryCard(e, onClick = { onOpen(e.id) }, onPin = { vm.setPinned(e.id, !e.pinned) }) } }
            }
            item {
                Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { export.launch("tnpscprep-diary.json") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.diary_export)) }
                        OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.diary_import)) }
                    }
                    Text(stringResource(R.string.diary_backup_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 12.dp + LocalBottomBarInset.current),
            icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.diary_new)) },
        )
    }
}

@Composable
private fun EntryCard(e: DiaryItem, onClick: () -> Unit, onPin: () -> Unit) {
    // the pin gives a small wiggle when it changes (not on first show)
    val wiggle = remember { androidx.compose.animation.core.Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(e.pinned) {
        if (first) { first = false; return@LaunchedEffect }
        wiggle.animateTo(-28f, tween(90)); wiggle.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.3f, stiffness = 400f))
    }
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 2.dp) {
        Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { DiaryMood(e.mood, 48.dp) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(e.date.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f, fill = false))
                    if (e.wrapup) Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(stringResource(R.string.diary_wrapup_tag), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(e.preview.trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            val label = stringResource(if (e.pinned) R.string.diary_unpin else R.string.diary_pin)
            IconButton(onClick = onPin, modifier = Modifier.semantics { contentDescription = label }) {
                Icon(
                    if (e.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin, null, modifier = Modifier.graphicsLayer { rotationZ = wiggle.value },
                    tint = if (e.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** "On this day": what she wrote on this date in earlier years. Hidden when there is nothing. */
@Composable
private fun MemoryCard(memories: List<com.spcode.tnpscprep.domain.Memory>, onOpen: (Long) -> Unit) {
    com.spcode.tnpscprep.ui.design.PopIn(0) {
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.memory_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                memories.forEach { m ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpen(m.item.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                        DiaryMood(m.item.mood, 40.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(androidx.compose.ui.res.pluralStringResource(R.plurals.memory_years_ago, m.yearsAgo, m.yearsAgo), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(m.item.preview.trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** The selected day in one place: what she did (study, food, steps), her diary entries and a way to write about it. */
@Composable
private fun DayStory(date: LocalDate, today: LocalDate, entries: List<DiaryItem>, onOpen: (Long) -> Unit, onWrite: (LocalDate) -> Unit, onWrapup: () -> Unit, wrapped: Boolean) {
    val app = LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp
    var recap by remember(date) { mutableStateOf<com.spcode.tnpscprep.domain.DayRecap?>(null) }
    LaunchedEffect(date) {
        recap = com.spcode.tnpscprep.domain.RecapBuilder.build(
            date, app.container.activityRepository.recapEvents(date), app.container.settingsStore.dailyGoalMinutes.value, emptyMap(), ZoneId.systemDefault(),
        )
    }
    val r = recap
    var plan by remember(date) { mutableStateOf<List<com.spcode.tnpscprep.data.db.PlannedTaskEntity>>(emptyList()) }
    LaunchedEffect(date) { plan = app.container.plannedTaskRepository.get(date) }
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())) + if (date == today) " · " + stringResource(R.string.cal_today) else "",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (r != null && !r.isEmpty) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (r.studyMinutes > 0) StoryChip(stringResource(R.string.story_study, r.studyMinutes))
                    if (r.topicsAdvancedCount > 0) StoryChip(stringResource(R.string.story_topics, r.topicsAdvancedCount))
                    if (r.affairsRevised > 0) StoryChip(stringResource(R.string.story_news, r.affairsRevised))
                    r.food?.let { f -> StoryChip(stringResource(R.string.ev_food, stringResource(when (f) { com.spcode.tnpscprep.domain.FoodState.HEALTHY -> R.string.food_healthy; com.spcode.tnpscprep.domain.FoodState.JUNK -> R.string.food_junk; com.spcode.tnpscprep.domain.FoodState.MIXED -> R.string.food_mixed }))) }
                    r.steps?.let { StoryChip(stringResource(R.string.story_steps, it)) }
                }
            }
            if (plan.isNotEmpty()) {
                val done = plan.count { it.done }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.spcode.tnpscprep.domain.PlanMood.of(done, plan.size, if (date == today) java.time.LocalTime.now().hour else 0)  .let { DiaryMood(it, 36.dp); Spacer(Modifier.padding(start = 10.dp)) }
                    Text(stringResource(R.string.story_plan) + " · $done/${plan.size}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                plan.forEach { t ->
                    Text(
                        (if (t.done) "✓ " else "○ ") + t.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (entries.isEmpty()) {
                Text(stringResource(R.string.story_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else entries.forEach { e ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onOpen(e.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    DiaryMood(e.mood, 36.dp)
                    Text(e.preview.trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp, top = 2.dp))
                    if (e.wrapup) Text(stringResource(R.string.diary_wrapup_tag), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onWrite(date) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.diary_write_about)) }
                if (date == today && !wrapped) OutlinedButton(onClick = onWrapup) { Text(stringResource(R.string.wrap_card_start)) }
            }
        }
    }
}

@Composable
private fun StoryChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** Counts only: no claims about feelings, no advice. */
@Composable
private fun ReviewCard(w: com.spcode.tnpscprep.domain.DiaryWeek, streak: Int) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.diary_review_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.diary_review_entries, w.entries), style = MaterialTheme.typography.bodyMedium)
            if (streak >= 2) Text(stringResource(R.string.diary_streak, streak), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            if (w.moods.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    w.moods.entries.sortedByDescending { it.value }.forEach { (m, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DiaryMood(m, 26.dp); Text("×$n", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Text(stringResource(R.string.diary_review_study, w.studyMinutes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---------------------------------------------------------------- editor

@Composable
private fun EditorPane(vm: DiaryViewModel, entryId: Long?, startDate: LocalDate, onDone: (removed: DiaryEntryEntity?) -> Unit, onWrapup: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val session = remember { EditorSession(null, null, null) }
    var loaded by remember { mutableStateOf(entryId == null) }
    var date by remember { mutableStateOf(startDate) }
    var text by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<String?>(null) }
    var lastEdit by remember { mutableStateOf(System.currentTimeMillis()) }
    var saved by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(entryId) {
        if (entryId != null) {
            vm.load(entryId)?.let { e ->
                session.id = e.id; session.savedText = e.text; session.savedMood = e.moodEmojiId
                text = e.text; mood = e.moodEmojiId; date = runCatching { LocalDate.parse(e.localDate) }.getOrDefault(date)
            }
            loaded = true
        } else focus.requestFocus()
    }
    // Autosave: this effect restarts on every keystroke, so the delay IS the debounce.
    LaunchedEffect(text, mood, loaded) {
        if (!loaded) return@LaunchedEffect
        lastEdit = System.currentTimeMillis()
        delay(com.spcode.tnpscprep.domain.Autosave.DEBOUNCE_MS)
        val r = vm.persist(session, date, text, mood, msSinceEdit = com.spcode.tnpscprep.domain.Autosave.DEBOUNCE_MS, leaving = false)
        if (r is Persisted.Saved) { if (r.first) haptic.performHapticFeedback(HapticFeedbackType.Confirm); saved = true }
    }
    LaunchedEffect(saved) { if (saved) { delay(1_800); saved = false } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (loaded) vm.flush(session, date, text, mood) }

    fun leave() { scope.launch { val r = vm.persist(session, date, text, mood, 0, leaving = true); onDone((r as? Persisted.Deleted)?.entry) } }
    BackHandler(onBack = ::leave)

    val prompts = stringArrayResource(R.array.diary_prompts)
    val prompt = prompts[DiaryPrompts.indexFor(LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault()), prompts.size)]
    val big = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 19.sp, lineHeight = 34.sp, color = MaterialTheme.colorScheme.onSurface,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), // Tamil signs are not clipped
    )

    val tint by androidx.compose.animation.animateColorAsState(com.spcode.tnpscprep.ui.wrapup.moodTint(mood), tween(500), label = "editorTint")
    Box(Modifier.fillMaxSize().background(tint)) {
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::leave) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.sheet_close)) }
            Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            // the chosen mood reacts (its own effect) right in the header
            val heroMood = mood?.let { n -> Mood.entries.firstOrNull { it.name == n } }
            AnimatedVisibility(heroMood != null, enter = androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(150)), exit = fadeOut(tween(150))) {
                if (heroMood != null) com.spcode.tnpscprep.ui.mood.MoodAvatar(heroMood, size = 56.dp)
            }
            AnimatedVisibility(saved, enter = fadeIn(tween(200)), exit = fadeOut(tween(400))) {
                Text(stringResource(R.string.diary_saved), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (session.id != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.diary_delete_title)) }
        }
        // one writing page, two modes: free writing (this page) or the guided evening wrap-up
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.editor_mode_free)) })
            androidx.compose.material3.FilterChip(selected = false, onClick = { scope.launch { vm.persist(session, date, text, mood, 0, leaving = true); onWrapup() } }, label = { Text(stringResource(R.string.editor_mode_wrapup)) })
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Mood.entries.forEach { m ->
                val sel = mood == m.name
                val pop by androidx.compose.animation.core.animateFloatAsState(if (sel) 1.2f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium), label = "moodPop")
                Box(
                    Modifier.size(52.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(CircleShape)
                        .border(BorderStroke(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), CircleShape)
                        .clickable { mood = if (sel) null else m.name },
                    contentAlignment = Alignment.Center,
                ) { DiaryMood(m.name, 44.dp) }
            }
        }
        if (text.isBlank() && loaded) {
            Surface(
                Modifier.padding(vertical = 8.dp).clip(RoundedCornerShape(50)).clickable { text = prompt + "\n\n" },
                shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer,
            ) { Text(prompt, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = text, onValueChange = { if (it.length <= DiaryLimits.MAX_CHARS) text = it }, enabled = loaded,
            textStyle = big, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
            decorationBox = { inner ->
                Box { if (text.isEmpty()) Text(stringResource(R.string.diary_write_hint), style = big.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))); inner() }
            },
        )
        if (text.isNotBlank()) {
            Text(
                stringResource(R.string.diary_words, com.spcode.tnpscprep.domain.DiaryStats.words(text)),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(vertical = 6.dp),
            )
        }
    }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.diary_delete_title)) },
            text = { Text(stringResource(R.string.diary_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; scope.launch { val id = session.id; onDone(if (id != null) vm.delete(id) else null) } }) {
                    Text(stringResource(R.string.health_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
