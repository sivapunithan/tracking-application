package com.spcode.tnpscprep.ui.diary

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onOpen: (Long) -> Unit, onNew: () -> Unit, onNewOn: (String) -> Unit,
) {
    var segment by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    var dayOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val app = LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp
    val fvm = androidx.lifecycle.viewmodel.compose.viewModel<com.spcode.tnpscprep.ui.today.FoodViewModel>(
        factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { com.spcode.tnpscprep.ui.today.FoodViewModel(app.container.foodRepository, app.container.settingsStore) } },
    )
    val food by fvm.days.collectAsStateWithLifecycle()
    val year by todayVm.yearHeat.collectAsStateWithLifecycle()
    val diaryItems by vm.items.collectAsStateWithLifecycle()
    val diaryDates = remember(diaryItems) { diaryItems.map { it.date }.toSet() }
    val today = remember { LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault()) }
    val undo by vm.undo.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    LaunchedEffect(undo) { if (undo != null) { delay(5_000); vm.clearUndo() } }
    LaunchedEffect(notice) { if (notice != null) { delay(4_000); vm.clearNotice() } }
    val deletedText = stringResource(R.string.diary_deleted)
    Surface(Modifier.fillMaxSize(), color = paper()) {
      Column(Modifier.fillMaxSize()) {
        val labels = listOf(R.string.journal_diary, R.string.journal_calendar)
        androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
            labels.forEachIndexed { i, res ->
                SegmentedButton(selected = segment == i, onClick = { segment = i }, shape = SegmentedButtonDefaults.itemShape(i, labels.size)) { Text(stringResource(res)) }
            }
        }
        Box(Modifier.fillMaxSize()) {
            if (segment == 0) TimelinePane(vm, studyByDate, onOpen = onOpen, onNew = onNew)
            else JournalCalendar(year.weeks, food, diaryDates, today, onDay = { dayOpen = it.toString() })
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
    val open = dayOpen?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (open != null) com.spcode.tnpscprep.ui.design.PopSheet(onDismiss = { dayOpen = null }) { dismiss ->
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            com.spcode.tnpscprep.ui.today.DaySheet(todayVm, open, dismiss, onDiary = { s ->
                dayOpen = null
                when {
                    s.startsWith("entry:") -> s.removePrefix("entry:").toLongOrNull()?.let(onOpen)
                    s.startsWith("new:") -> onNewOn(s.removePrefix("new:"))
                    else -> onNew()
                }
            })
        }
    }
}

/** One diary entry as a full-screen page. [entryId] = open that entry; otherwise a new entry for [dateArg] (yyyy-MM-dd) or today's logical day. */
@Composable
fun JournalEditorScreen(vm: DiaryViewModel, entryId: Long?, dateArg: String?, onDone: (removed: DiaryEntryEntity?) -> Unit) {
    val date = remember(dateArg) {
        dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault())
    }
    Surface(Modifier.fillMaxSize(), color = paper()) { EditorPane(vm, entryId, date, onDone) }
}

// ---------------------------------------------------------------- mood emoji (reuses the mood avatars)

@Composable
fun DiaryMood(id: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val res = remember(id) { Mood.entries.firstOrNull { it.name == id }?.let { moodImageRes(ctx, it) } ?: 0 }
    if (res != 0) {
        Image(painterResource(res), null, modifier.size(size), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
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
    vm: DiaryViewModel, studyByDate: Map<LocalDate, Int>, onOpen: (Long) -> Unit, onNew: () -> Unit,
) {
    val onNotice: (String) -> Unit = vm::postNotice
    val ctx = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val groups = remember(items) { DiaryTimeline.groupByMonth(items) }
    val today = remember { LogicalDay.of(System.currentTimeMillis(), ZoneId.systemDefault()) }
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
            item {
                OutlinedTextField(
                    value = query, onValueChange = { vm.query.value = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.diary_search_hint)) }, shape = RoundedCornerShape(16.dp),
                )
            }
            if (query.isBlank() && items.isNotEmpty()) item { ReviewCard(week) }
            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(if (query.isBlank()) R.string.diary_empty else R.string.diary_no_results),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            groups.forEach { g ->
                item(key = "m-" + g.month) {
                    Text(
                        g.month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(g.items, key = { it.id }) { e -> EntryCard(e, onClick = { onOpen(e.id) }) }
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
private fun EntryCard(e: DiaryItem, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 1.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { DiaryMood(e.mood, 40.dp) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(e.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(e.preview.trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** Counts only: no claims about feelings, no advice. */
@Composable
private fun ReviewCard(w: com.spcode.tnpscprep.domain.DiaryWeek) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.diary_review_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.diary_review_entries, w.entries), style = MaterialTheme.typography.bodyMedium)
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
private fun EditorPane(vm: DiaryViewModel, entryId: Long?, startDate: LocalDate, onDone: (removed: DiaryEntryEntity?) -> Unit) {
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

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::leave) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.sheet_close)) }
            Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            AnimatedVisibility(saved, enter = fadeIn(tween(200)), exit = fadeOut(tween(400))) {
                Text(stringResource(R.string.diary_saved), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (session.id != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.diary_delete_title)) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Mood.entries.forEach { m ->
                val sel = mood == m.name
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
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
