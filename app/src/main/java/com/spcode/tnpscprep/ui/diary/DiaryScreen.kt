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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Link
import com.spcode.tnpscprep.data.db.DiaryPhotoEntity
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
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
    val undoCtx = LocalContext.current
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
                            TextButton(onClick = { com.spcode.tnpscprep.ui.mood.MoodSounds.play(undoCtx, com.spcode.tnpscprep.domain.MoodSoundRules.UNDONE); u?.let(vm::restore); vm.clearUndo() }) { Text(stringResource(R.string.diary_undo), color = MaterialTheme.colorScheme.inversePrimary) }
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
    // first-time tour: targets report their window bounds; the tour starts a moment after the page is up
    val gBounds = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val jGuideDone by (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore.journalGuideDone.collectAsStateWithLifecycle()
    var guideReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(900); guideReady = true }
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
                Box(Modifier.onGloballyPositioned { gBounds["wrapup"] = it.boundsInWindow() }) {
                    com.spcode.tnpscprep.ui.wrapup.WrapupCardView(if (wrapped) com.spcode.tnpscprep.domain.WrapupCardState.DONE else com.spcode.tnpscprep.domain.WrapupCardState.INVITE, onWrapup, lastSeven)
                }
            }
            if (query.isBlank()) {
                val memories = DiaryMemories.onThisDay(items, today)
                if (memories.isNotEmpty()) item(key = "memories") { MemoryCard(memories, onOpen) }
                item(key = "calendar") {
                    Box(Modifier.onGloballyPositioned { gBounds["calendar"] = it.boundsInWindow() }) { JournalCalendarCard(weeks, food, moods, items.map { it.date }.toSet(), today, selected, onSelect) }
                }
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
            onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 12.dp + LocalBottomBarInset.current).onGloballyPositioned { gBounds["new"] = it.boundsInWindow() },
            icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.diary_new)) },
        )
        if (!jGuideDone && guideReady && query.isBlank()) com.spcode.tnpscprep.ui.guide.CoachOverlay(
            listOf(
                com.spcode.tnpscprep.ui.guide.CoachStep("wrapup", stringResource(R.string.guide_j1_title), stringResource(R.string.guide_j1_body)),
                com.spcode.tnpscprep.ui.guide.CoachStep("calendar", stringResource(R.string.guide_j2_title), stringResource(R.string.guide_j2_body)),
                com.spcode.tnpscprep.ui.guide.CoachStep("new", stringResource(R.string.guide_j3_title), stringResource(R.string.guide_j3_body)),
            ),
            gBounds, onFinish = { (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore.setJournalGuideDone(true) },
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
            if (e.firstPhoto != null) EntryThumb(e.firstPhoto, e.photoCount)
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

/** The entry's first photo as a small thumbnail (the 320 px copy, decoded off the main thread and cached), with "+n" when there are more. */
@Composable
fun EntryThumb(name: String, count: Int, size: androidx.compose.ui.unit.Dp = 64.dp) {
    val store = (LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp).container.photoStore
    val bmp by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, name) {
        value = PhotoBitmaps.get(store, name, thumb = true)?.asImageBitmap()
    }
    Box(Modifier.padding(start = 6.dp).size(size).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        bmp?.let { androidx.compose.foundation.Image(it, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
        if (count > 1) Surface(Modifier.align(Alignment.BottomEnd).padding(3.dp), shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.6f)) {
            Text("+${count - 1}", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
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
                    if (e.firstPhoto != null) EntryThumb(e.firstPhoto, e.photoCount, 44.dp)
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

/** [block] = display index of the text run; [link] null = a new link. */
private data class LinkEdit(val block: Int, val link: com.spcode.tnpscprep.domain.DiaryLinks.Link?)

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
    val ctx = LocalContext.current
    val eBounds = remember { androidx.compose.runtime.mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val eGuideDone by (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore.editorGuideDone.collectAsStateWithLifecycle()
    var eGuideReady by remember { mutableStateOf(false) }
    val focusMgr = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(Unit) { delay(1100); eGuideReady = true }
    LaunchedEffect(eGuideReady, eGuideDone) { if (eGuideReady && !eGuideDone) focusMgr.clearFocus() }   // hides the keyboard while the tour is up
    val photos = remember { androidx.compose.runtime.mutableStateMapOf<String, DiaryPhotoEntity>() }
    var pendingPhoto by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var removePhoto by remember { mutableStateOf<String?>(null) }
    var editPhoto by remember { mutableStateOf<String?>(null) }
    var linkEdit by remember { mutableStateOf<LinkEdit?>(null) }
    var cursorBlock by remember { mutableStateOf(-1) }   // display index of the text block that has the cursor
    var cursorPos by remember { mutableStateOf(0) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val b = withContext(kotlinx.coroutines.Dispatchers.IO) { vm.store.decode(uri) }
            if (b == null) android.widget.Toast.makeText(ctx, ctx.getString(R.string.photo_read_fail), android.widget.Toast.LENGTH_SHORT).show() else pendingPhoto = b
        }
    }

    LaunchedEffect(entryId) {
        if (entryId != null) {
            vm.load(entryId)?.let { e ->
                session.id = e.id; session.savedText = e.text; session.savedMood = e.moodEmojiId
                text = e.text; mood = e.moodEmojiId; date = runCatching { LocalDate.parse(e.localDate) }.getOrDefault(date)
                vm.photosFor(e.id).forEach { photos[it.name] = it; session.photoFiles += it.name }
                session.savedSig = photoSig(photos.values)
            }
            loaded = true
        } else focus.requestFocus()
    }
    // Autosave: this effect restarts on every keystroke, so the delay IS the debounce.
    val sig = photoSig(photos.values)
    LaunchedEffect(text, mood, loaded, sig) {
        if (!loaded) return@LaunchedEffect
        lastEdit = System.currentTimeMillis()
        delay(com.spcode.tnpscprep.domain.Autosave.DEBOUNCE_MS)
        val r = vm.persist(session, date, text, mood, msSinceEdit = com.spcode.tnpscprep.domain.Autosave.DEBOUNCE_MS, leaving = false, photos = photos.values.toList())
        if (r is Persisted.Saved) { if (r.first) haptic.performHapticFeedback(HapticFeedbackType.Confirm); saved = true }
    }
    LaunchedEffect(saved) { if (saved) { delay(1_800); saved = false } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (loaded) vm.flush(session, date, text, mood, photos.values.toList()) }

    fun leave() { scope.launch { val r = vm.persist(session, date, text, mood, 0, leaving = true, photos = photos.values.toList()); onDone((r as? Persisted.Deleted)?.entry) } }
    BackHandler(onBack = ::leave)

    val prompts = stringArrayResource(R.array.diary_prompts)
    val prompt = prompts[DiaryPrompts.indexFor(LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault()), prompts.size)]
    val big = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 19.sp, lineHeight = 34.sp, color = MaterialTheme.colorScheme.onSurface,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), // Tamil signs are not clipped
    )

    // The entry = blocks: runs of text and photos. [display] adds empty text runs before/after photos so she can always type there;
    // those are "virtual": dropped again when saving, so no stray blank lines appear in the stored text.
    val real = remember(text) { com.spcode.tnpscprep.domain.DiaryPhotos.parse(text) }
    class Disp(val block: com.spcode.tnpscprep.domain.DiaryBlock, val virtual: Boolean)
    val display = remember(real) {
        buildList {
            if (real.first() is com.spcode.tnpscprep.domain.DiaryBlock.Photo) add(Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(""), true))
            real.forEachIndexed { i, b ->
                add(Disp(b, false))
                if (b is com.spcode.tnpscprep.domain.DiaryBlock.Photo && real.getOrNull(i + 1) !is com.spcode.tnpscprep.domain.DiaryBlock.Text) add(Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(""), true))
            }
        }
    }
    fun commit(list: List<Disp>) {
        val out = list.filterNot { it.virtual && (it.block as? com.spcode.tnpscprep.domain.DiaryBlock.Text)?.text.isNullOrEmpty() }.map { it.block }
        val t = com.spcode.tnpscprep.domain.DiaryPhotos.join(out)
        if (t.length <= DiaryLimits.MAX_CHARS) text = t
    }
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
            androidx.compose.material3.FilterChip(selected = false, onClick = { scope.launch { vm.persist(session, date, text, mood, 0, leaving = true, photos = photos.values.toList()); onWrapup() } }, label = { Text(stringResource(R.string.editor_mode_wrapup)) })
        }
        Row(Modifier.fillMaxWidth().onGloballyPositioned { eBounds["mood"] = it.boundsInWindow() }.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Mood.entries.forEach { m ->
                val sel = mood == m.name
                val pop by androidx.compose.animation.core.animateFloatAsState(if (sel) 1.2f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium), label = "moodPop")
                Box(
                    Modifier.size(52.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(CircleShape)
                        .border(BorderStroke(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), CircleShape)
                        .clickable { mood = if (sel) null else m.name; com.spcode.tnpscprep.ui.mood.MoodSounds.play(ctx, com.spcode.tnpscprep.domain.MoodSoundRules.forMood(mood)) },
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
        val scroll = rememberScrollState()
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll)) {
            display.forEachIndexed { i, d ->
                when (val b = d.block) {
                    is com.spcode.tnpscprep.domain.DiaryBlock.Text -> EditorTextBlock(
                        b.text, big, if (display.size == 1) stringResource(R.string.diary_write_hint) else null,
                        onText = { n -> commit(display.toMutableList().also { it[i] = Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(n), false) }) },
                        onCursor = { cursorPos = it }, onFocus = { cursorBlock = i },
                        onLink = { link, long ->
                            if (long) linkEdit = LinkEdit(i, link)
                            else runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link.url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        },
                        modifier = (if (i == 0) Modifier.focusRequester(focus) else Modifier).let { m -> if (loaded) m else m },
                    )
                    is com.spcode.tnpscprep.domain.DiaryBlock.Photo -> PhotoBlockView(b.name, photos[b.name], vm.store) { editPhoto = b.name }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(modifier = Modifier.onGloballyPositioned { eBounds["photo"] = it.boundsInWindow() }, onClick = {
                if (photos.size >= com.spcode.tnpscprep.domain.DiaryPhotos.MAX_PER_ENTRY) android.widget.Toast.makeText(ctx, ctx.getString(R.string.photo_limit, com.spcode.tnpscprep.domain.DiaryPhotos.MAX_PER_ENTRY), android.widget.Toast.LENGTH_SHORT).show()
                else picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.editor_add_photo))
            }
            TextButton(modifier = Modifier.onGloballyPositioned { eBounds["link"] = it.boundsInWindow() }, onClick = { linkEdit = LinkEdit(-1, null) }) {
                Icon(Icons.Rounded.Link, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.link_add))
            }
            Spacer(Modifier.weight(1f))
            if (text.isNotBlank()) Text(
                stringResource(R.string.diary_words, com.spcode.tnpscprep.domain.DiaryStats.words(text)),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
        if (!eGuideDone && eGuideReady && loaded) com.spcode.tnpscprep.ui.guide.CoachOverlay(
            listOf(
                com.spcode.tnpscprep.ui.guide.CoachStep("mood", stringResource(R.string.guide_e1_title), stringResource(R.string.guide_e1_body)),
                com.spcode.tnpscprep.ui.guide.CoachStep("photo", stringResource(R.string.guide_e2_title), stringResource(R.string.guide_e2_body)),
                com.spcode.tnpscprep.ui.guide.CoachStep("link", stringResource(R.string.guide_e3_title), stringResource(R.string.guide_e3_body)),
            ),
            eBounds, onFinish = { (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore.setEditorGuideDone(true) },
        )
    }

    pendingPhoto?.let { bmp ->
        PhotoCropDialog(bmp, onCancel = { pendingPhoto = null }) { out, frame, corner ->
            pendingPhoto = null
            scope.launch {
                val name = com.spcode.tnpscprep.domain.DiaryPhotos.newName()
                val saved = withContext(kotlinx.coroutines.Dispatchers.IO) { vm.store.write(name, out) }
                if (saved == null) { android.widget.Toast.makeText(ctx, ctx.getString(R.string.photo_read_fail), android.widget.Toast.LENGTH_SHORT).show(); return@launch }
                com.spcode.tnpscprep.ui.mood.MoodSounds.play(ctx, com.spcode.tnpscprep.domain.MoodSoundRules.PHOTO)
                photos[name] = DiaryPhotoEntity(name, 0, frame, corner, saved.width, saved.height, saved.bytes, System.currentTimeMillis())
                session.photoFiles += name
                // place it at the cursor: split that text run in two (a newline at the cut is not kept), else at the end
                val list = display.toMutableList()
                val at = cursorBlock.takeIf { it in list.indices && list[it].block is com.spcode.tnpscprep.domain.DiaryBlock.Text }
                val photo = Disp(com.spcode.tnpscprep.domain.DiaryBlock.Photo(name), false)
                if (at == null) list += photo
                else {
                    val t = (list[at].block as com.spcode.tnpscprep.domain.DiaryBlock.Text).text
                    val cut = cursorPos.coerceIn(0, t.length)
                    val before = t.substring(0, cut).removeSuffix("\n"); val after = t.substring(cut).removePrefix("\n")
                    list.removeAt(at)
                    var k = at
                    if (before.isNotEmpty()) list.add(k++, Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(before), false))
                    list.add(k++, photo)
                    if (after.isNotEmpty()) list.add(k, Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(after), false))
                }
                commit(list)
            }
        }
    }
    // photo edit sheet: frame, move, remove
    editPhoto?.let { name ->
        val meta = photos[name]
        val idx = real.indexOfFirst { (it as? com.spcode.tnpscprep.domain.DiaryBlock.Photo)?.name == name }
        if (meta == null || idx < 0) editPhoto = null
        else PhotoEditSheet(
            meta, canUp = idx > 0, canDown = idx < real.size - 1,
            onChange = { f, c -> photos[name] = meta.copy(frame = f.name, cornerDp = c) },
            onUp = { val l = real.toMutableList(); java.util.Collections.swap(l, idx, idx - 1); text = com.spcode.tnpscprep.domain.DiaryPhotos.join(l) },
            onDown = { val l = real.toMutableList(); java.util.Collections.swap(l, idx, idx + 1); text = com.spcode.tnpscprep.domain.DiaryPhotos.join(l) },
            onRemove = { editPhoto = null; removePhoto = name },
            onDismiss = { editPhoto = null },
        )
    }
    // link dialog: new link at the cursor, or edit/open/remove the long-pressed one
    linkEdit?.let { le ->
        var label by remember(le) { mutableStateOf(le.link?.label ?: "") }
        var url by remember(le) { mutableStateOf(le.link?.url ?: "") }
        var bad by remember(le) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { linkEdit = null },
            title = { Text(stringResource(if (le.link == null) R.string.link_title else R.string.link_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(label, { label = it.take(80) }, singleLine = true, label = { Text(stringResource(R.string.link_label)) })
                    androidx.compose.material3.OutlinedTextField(url, { url = it; bad = false }, singleLine = true, isError = bad, label = { Text(stringResource(R.string.link_url)) },
                        supportingText = if (bad) ({ Text(stringResource(R.string.link_bad)) }) else null)
                    if (le.link != null) Row {
                        TextButton(onClick = { runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(le.link.url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }) { Text(stringResource(R.string.link_open)) }
                        TextButton(onClick = {
                            val l = display.toMutableList(); val d = l.getOrNull(le.block)
                            val t = (d?.block as? com.spcode.tnpscprep.domain.DiaryBlock.Text)?.text
                            if (t != null) { l[le.block] = Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(t.replaceRange(le.link.start, le.link.end, le.link.label)), false); commit(l) }
                            linkEdit = null
                        }) { Text(stringResource(R.string.link_unlink), color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val md = com.spcode.tnpscprep.domain.DiaryLinks.make(label.ifBlank { url }, url)
                    if (md == null) { bad = true; return@TextButton }
                    val l = display.toMutableList()
                    if (le.link != null) {
                        val t = (l.getOrNull(le.block)?.block as? com.spcode.tnpscprep.domain.DiaryBlock.Text)?.text
                        if (t != null) l[le.block] = Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(t.replaceRange(le.link.start, le.link.end, md)), false)
                    } else {
                        // new link: at the cursor of the text run she was in, followed by a space so she can keep typing after it
                        val at = cursorBlock.takeIf { it in l.indices && l[it].block is com.spcode.tnpscprep.domain.DiaryBlock.Text }
                            ?: l.indexOfLast { it.block is com.spcode.tnpscprep.domain.DiaryBlock.Text }
                        if (at >= 0) {
                            val t = (l[at].block as com.spcode.tnpscprep.domain.DiaryBlock.Text).text
                            val cut = if (at == cursorBlock) cursorPos.coerceIn(0, t.length) else t.length
                            l[at] = Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text(t.substring(0, cut) + md + " " + t.substring(cut)), false)
                        } else l += Disp(com.spcode.tnpscprep.domain.DiaryBlock.Text("$md "), false)
                    }
                    commit(l); linkEdit = null
                }) { Text(stringResource(if (le.link == null) R.string.link_insert else R.string.link_save)) }
            },
            dismissButton = { TextButton(onClick = { linkEdit = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    removePhoto?.let { name ->
        AlertDialog(
            onDismissRequest = { removePhoto = null },
            title = { Text(stringResource(R.string.photo_remove_title)) },
            text = { Text(stringResource(R.string.photo_remove_body)) },
            confirmButton = { TextButton(onClick = {
                val n = name; removePhoto = null
                commit(display.filterNot { (it.block as? com.spcode.tnpscprep.domain.DiaryBlock.Photo)?.name == n })
                PhotoBitmaps.forget(n); com.spcode.tnpscprep.ui.mood.MoodSounds.play(ctx, com.spcode.tnpscprep.domain.MoodSoundRules.DELETED)
            }) { Text(stringResource(R.string.health_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { removePhoto = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.diary_delete_title)) },
            text = { Text(stringResource(R.string.diary_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; com.spcode.tnpscprep.ui.mood.MoodSounds.play(ctx, com.spcode.tnpscprep.domain.MoodSoundRules.DELETED); scope.launch { val id = session.id; onDone(if (id != null) vm.delete(id) else null) } }) {
                    Text(stringResource(R.string.health_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
