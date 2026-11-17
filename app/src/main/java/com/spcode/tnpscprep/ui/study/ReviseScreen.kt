package com.spcode.tnpscprep.ui.study

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.spcode.tnpscprep.ui.theme.LocalIsDark
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

import com.spcode.tnpscprep.ui.today.DueSheet
import com.spcode.tnpscprep.ui.today.EmptyHint
import com.spcode.tnpscprep.ui.today.SectionTitle
import com.spcode.tnpscprep.ui.today.StarSheet
import com.spcode.tnpscprep.ui.today.TodayViewModel

/**
 * Study > Revise: everything to revisit, moved here from the Today screen unchanged: due revisions (topics + current affairs),
 * progress by subject, and starred topics. (A richer layout comes with the Syllabus redesign.)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviseScreen(vm: TodayViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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

    val current = sheet
    if (current != null) {
        PopSheet(onDismiss = { sheet = null }) { dismiss ->
            when {
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
