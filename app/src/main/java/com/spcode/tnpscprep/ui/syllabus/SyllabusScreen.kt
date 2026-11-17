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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.spcode.tnpscprep.ui.localized

/** [twoPane] = list + detail side by side (tablet/wide); otherwise list -> detail with Back (phone). */
@Composable
fun SyllabusScreen(viewModel: SyllabusViewModel, twoPane: Boolean) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    // rememberSaveable: survives rotation AND process death (ColorOS kills background apps).
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
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
        Header(filter, onFilter = viewModel::setFilter)
        HorizontalDivider()
        when {
            subjects.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.no_topics), style = MaterialTheme.typography.bodyLarge)
            }
            twoPane -> Row(Modifier.fillMaxSize()) {
                SubjectList(subjects, current?.id, { selectedId = it }, Modifier.weight(0.4f).fillMaxHeight())
                VerticalDivider()
                if (current != null) SubjectDetail(current, viewModel, onOpenNote, showBack = false, onBack = {}, Modifier.weight(0.6f).fillMaxHeight())
            }
            current == null -> SubjectList(subjects, null, { selectedId = it }, Modifier.fillMaxSize())
            else -> {
                BackHandler { selectedId = null }
                SubjectDetail(current, viewModel, onOpenNote, showBack = true, onBack = { selectedId = null }, Modifier.fillMaxSize())
            }
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
private fun Header(filter: ExamFilter, onFilter: (ExamFilter) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExamFilter.entries.forEach { f ->
                FilterChip(
                    selected = f == filter,
                    onClick = { onFilter(f) },
                    label = { Text(if (f == ExamFilter.ALL) stringResource(R.string.filter_all) else f.name) },
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(R.string.hint_tap_longpress),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubjectList(
    subjects: List<SubjectUi>, selectedId: String?, onSelect: (String) -> Unit, modifier: Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    LinearProgressIndicator(
                        progress = { s.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        // Default track is almost the card's own colour, so an empty bar looked like a dot.
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
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
    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
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
                TopicRow(
                    t,
                    onTap = { vm.onTopicTap(t.id) },
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
