package com.spcode.tnpscprep.ui.syllabus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.spcode.tnpscprep.data.db.StudiedTopic
import com.spcode.tnpscprep.domain.ExamSelection
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.Exam
import com.spcode.tnpscprep.domain.RevisionLevel
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.ScreenTitle
import com.spcode.tnpscprep.ui.localized

/** [twoPane] = list + detail side by side (tablet/wide); otherwise list -> detail with Back (phone). */
@Composable
fun SyllabusScreen(viewModel: SyllabusViewModel, twoPane: Boolean, initialSubject: String? = null, onHome: (() -> Unit)? = null) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val examMask by viewModel.examMask.collectAsStateWithLifecycle()
    val lastTouched by viewModel.lastTouched.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showExams by rememberSaveable { mutableStateOf(false) }
    val hits = remember(subjects, query) { searchTopics(subjects, query) }
    // rememberSaveable: survives rotation AND process death (ColorOS kills background apps).
    var selectedId by rememberSaveable { mutableStateOf(initialSubject) }
    // Note dialog state is saveable too, so rotating while typing doesn't lose the text.
    var noteTopicId by rememberSaveable { mutableStateOf<String?>(null) }
    var noteText by rememberSaveable { mutableStateOf("") }
    var noteLoaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(noteTopicId) {
        val id = noteTopicId
        if (id != null && !noteLoaded) { noteText = viewModel.loadNote(id); noteLoaded = true }
    }
    val onOpenNote: (String) -> Unit = { id -> noteText = ""; noteLoaded = false; noteTopicId = id }

    val current = subjects.firstOrNull { it.id == selectedId }
        ?: if (twoPane) subjects.firstOrNull() else null

    Column(Modifier.fillMaxSize()) {
        Header(examMask, query, onQuery = { query = it }, onExams = { showExams = true })
        HorizontalDivider()
        when {
            query.isNotBlank() -> SearchResults(hits, Modifier.fillMaxSize()) { id -> selectedId = id; query = "" }
            subjects.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.no_topics), style = MaterialTheme.typography.bodyLarge)
            }
            twoPane -> Row(Modifier.fillMaxSize()) {
                SubjectList(subjects, current?.id, { selectedId = it }, Modifier.weight(0.4f).fillMaxHeight(), lastTouched) { selectedId = it }
                VerticalDivider()
                if (current != null) SubjectDetail(current, viewModel, onOpenNote, showBack = false, onBack = {}, Modifier.weight(0.6f).fillMaxHeight())
            }
            current == null -> SubjectList(subjects, null, { selectedId = it }, Modifier.fillMaxSize(), lastTouched) { selectedId = it }
            else -> {
                val back: () -> Unit = { if (onHome != null) onHome() else selectedId = null }
                BackHandler(onBack = back)
                SubjectDetail(current, viewModel, onOpenNote, showBack = true, onBack = back, Modifier.fillMaxSize())
            }
        }
    }

    if (showExams) com.spcode.tnpscprep.ui.design.PopSheet(onDismiss = { showExams = false }) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.my_exams), style = MaterialTheme.typography.titleLarge)
            ExamPicker(examMask, viewModel::setExamMask)
            Text(stringResource(R.string.my_exams_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(16.dp))
        }
    }

    val noteTopic = subjects.asSequence().flatMap { it.units }.flatMap { it.topics }.firstOrNull { it.id == noteTopicId }
    if (noteTopic != null && noteLoaded) {
        AlertDialog(
            onDismissRequest = { noteTopicId = null },
            title = { Text(localized(noteTopic.nameEn, noteTopic.nameTa), style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(stringResource(R.string.note_hint)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onSaveNote(noteTopic.id, noteText); noteTopicId = null }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = { TextButton(onClick = { noteTopicId = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Header(mask: Int, query: String, onQuery: (String) -> Unit, onExams: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ScreenTitle(stringResource(R.string.syllabus_title)) }
            val label = if (mask == Exam.ALL_MASK) stringResource(R.string.my_exams_all) else ExamSelection.codes(mask).joinToString(" · ")
            AssistChip(onClick = onExams, label = { Text(stringResource(R.string.my_exams) + ": " + label) })
        }
        OutlinedTextField(
            value = query, onValueChange = { onQuery(it.take(60)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.syllabus_search_hint)) },
            trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { onQuery("") }) { Text(stringResource(R.string.clear)) } },
        )
        Text(stringResource(R.string.hint_tap_longpress), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchResults(hits: List<TopicHit>, modifier: Modifier, onOpen: (String) -> Unit) {
    if (hits.isEmpty()) {
        Box(modifier, Alignment.Center) { Text(stringResource(R.string.syllabus_search_none), style = MaterialTheme.typography.bodyLarge) }
        return
    }
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 16.dp + LocalBottomBarInset.current)) {
        items(hits, key = { it.topic.id }) { h ->
            Column(Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(h.subjectId) }).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(localized(h.topic.nameEn, h.topic.nameTa), style = MaterialTheme.typography.bodyLarge)
                Text(localized(h.subjectNameEn, h.subjectNameTa), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Stacked bar: one segment per revision level (0 = not started, light; higher levels = stronger crimson). Draws nothing odd at 0%. */
@Composable
internal fun LevelBar(counts: List<Int>, modifier: Modifier = Modifier) {
    val total = counts.sum().coerceAtLeast(1)
    val base = MaterialTheme.colorScheme.primary
    val colors = listOf(MaterialTheme.colorScheme.outlineVariant) + (1..RevisionLevel.MAX).map { base.copy(alpha = 0.3f + 0.7f * it / RevisionLevel.MAX) }
    Row(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).clearAndSetSemantics {}) {
        counts.forEachIndexed { level, n ->
            if (n > 0) Box(Modifier.weight(n.toFloat() / total).fillMaxHeight().background(colors.getOrElse(level) { base }))
        }
    }
}

@Composable
private fun SubjectList(
    subjects: List<SubjectUi>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier,
    lastTouched: StudiedTopic?, onContinue: (String) -> Unit,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalBottomBarInset.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val resume = lastTouched?.let { t -> subjects.firstOrNull { it.id == t.subjectId }?.let { subj -> t to subj } }
        if (resume != null) item(key = "continue") {
            val (t, subj) = resume
            Card(onClick = { onContinue(subj.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.continue_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(localized(t.nameEn, t.nameTa), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(localized(subj.nameEn, subj.nameTa), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        items(subjects, key = { it.id }) { s ->
            val selected = s.id == selectedId
            Card(
                onClick = { onSelect(s.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(localized(s.nameEn, s.nameTa), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.percent_format, s.percent), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.size(8.dp))
                    val started = s.topicCount - s.levelCounts.firstOrNull().let { it ?: 0 }
                    val barDesc = stringResource(R.string.bar_started, started, s.topicCount)
                    LevelBar(s.levelCounts, Modifier.semantics { contentDescription = barDesc })
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(R.string.topic_count, s.topicCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectDetail(
    subject: SubjectUi, vm: SyllabusViewModel, onOpenNote: (String) -> Unit, showBack: Boolean, onBack: () -> Unit, modifier: Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 24.dp + LocalBottomBarInset.current)) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (showBack) TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(localized(subject.nameEn, subject.nameTa), style = MaterialTheme.typography.titleLarge)
            }
        }
        subject.units.forEach { unit ->
            item(key = unit.id) {
                Text(
                    localized(unit.nameEn, unit.nameTa),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(unit.topics, key = { it.id }) { t ->
                val sctx = androidx.compose.ui.platform.LocalContext.current
                TopicRow(
                    t,
                    onTap = { vm.onTopicTap(t.id); if (t.level < 4) com.spcode.tnpscprep.ui.mood.MoodSounds.play(sctx, com.spcode.tnpscprep.domain.MoodSoundRules.forTopicLevelUp(t.level + 1)) },
                    onLongPress = { vm.onTopicLongPress(t.id) },
                    onStar = { vm.onToggleStar(t.id) },
                    onNote = { onOpenNote(t.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopicRow(t: TopicUi, onTap: () -> Unit, onLongPress: () -> Unit, onStar: () -> Unit, onNote: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val levelName = stringArrayResource(R.array.revision_levels)[RevisionLevel.clamp(t.level)]
    val exams = Exam.entries.filter { t.exams and it.bit != 0 }.joinToString(" · ") { it.code }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(
                onClickLabel = stringResource(R.string.a11y_advance),
                onClick = onTap,
                onLongClickLabel = stringResource(R.string.a11y_reset),
                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress() },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(localized(t.nameEn, t.nameTa), style = MaterialTheme.typography.bodyLarge)
            Text(
                "$levelName  ·  $exams",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(4.dp))
        // Weak-topic star and own note. Text glyphs instead of an icon library: no extra dependency.
        val starLabel = stringResource(if (t.starred) R.string.a11y_unstar else R.string.a11y_star)
        TextButton(
            onClick = onStar, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(44.dp).semantics { contentDescription = starLabel },
        ) { Text(if (t.starred) "★" else "☆", style = MaterialTheme.typography.titleLarge) }
        val noteLabel = stringResource(R.string.note_title)
        TextButton(
            onClick = onNote, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(44.dp).semantics { contentDescription = noteLabel },
        ) {
            Text(
                "✎", style = MaterialTheme.typography.titleLarge,
                color = if (t.hasNote) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(4.dp))
        // Decorative: the text label above already says the level, so hide the dots from screen readers.
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(RevisionLevel.MAX) { i ->
                Box(
                    Modifier.size(12.dp).clip(CircleShape).background(
                        if (i < t.level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }
        }
    }
}
