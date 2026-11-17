package com.spcode.tnpscprep.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.RevisionLevel
import com.spcode.tnpscprep.ui.localized
import com.spcode.tnpscprep.ui.syllabus.ExamFilter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: TodayViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.target_exam_label), style = MaterialTheme.typography.titleSmall)
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
        }

        // --- countdown + readiness ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val days = ui.daysToExam
                    val date = ui.examDate
                    when {
                        date == null -> Text(stringResource(R.string.exam_date_not_set), style = MaterialTheme.typography.titleMedium)
                        days != null && days >= 0 -> Text(
                            pluralStringResource(R.plurals.exam_in_days, days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), days.toInt()) +
                                "  ·  " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        else -> Text(stringResource(R.string.exam_date_passed), style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showPicker = true }) { Text(stringResource(R.string.set_exam_date)) }
                        if (date != null) TextButton(onClick = vm::clearExamDate) { Text(stringResource(R.string.clear)) }
                    }
                    Text(stringResource(R.string.readiness_label, ui.progress.readinessPercent), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(
                        progress = { ui.progress.readinessPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        stringResource(R.string.not_started_count, ui.progress.notStarted, ui.progress.totalTopics),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // --- today's study ---
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.today_label, ui.todayMinutes, ui.goalMinutes), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { (ui.todayMinutes.toFloat() / ui.goalMinutes).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(pluralStringResource(R.plurals.streak_days, ui.streak, ui.streak), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // --- due for revision ---
        item {
            Text(
                stringResource(R.string.due_title, ui.progress.due.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (ui.progress.due.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.due_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(ui.progress.due, key = { it.topicId }) { d ->
                val levelName = stringArrayResource(R.array.revision_levels)[RevisionLevel.clamp(d.level)]
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(localized(d.nameEn, d.nameTa), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                localized(d.subjectEn, d.subjectTa),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                levelName + "  ·  " + if (d.daysOverdue == 0) stringResource(R.string.due_today)
                                else pluralStringResource(R.plurals.overdue_days, d.daysOverdue, d.daysOverdue),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (d.daysOverdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Button(onClick = { vm.markRevised(d.topicId) }) { Text(stringResource(R.string.revised)) }
                    }
                }
            }
        }

        // --- progress by subject ---
        if (ui.progress.subjects.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.subject_progress_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(ui.progress.subjects, key = { "subj-" + it.subjectId }) { sp ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(localized(sp.nameEn, sp.nameTa), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.percent_format, sp.percent), style = MaterialTheme.typography.bodyMedium)
                    }
                    LinearProgressIndicator(
                        progress = { sp.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }

        // --- starred (weak) topics ---
        item {
            Text(
                stringResource(R.string.starred_title, ui.progress.starred.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (ui.progress.starred.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.starred_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(ui.progress.starred, key = { "star-" + it.topicId }) { st ->
                val levelName = stringArrayResource(R.array.revision_levels)[RevisionLevel.clamp(st.level)]
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(localized(st.nameEn, st.nameTa), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                localized(st.subjectEn, st.subjectTa) + "  ·  " + levelName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (st.note.isNotBlank()) {
                                Text(st.note, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                        TextButton(onClick = { vm.toggleStar(st.topicId) }, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp)) {
                            Text("★", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {  // (date picker, below the list)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = ui.examDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // The picker works in UTC midnight; convert back to a plain calendar date.
                    state.selectedDateMillis?.let { vm.setExamDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
                    showPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }
}
