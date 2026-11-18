package com.spcode.tnpscprep.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import com.spcode.tnpscprep.ui.guide.guideTarget
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.Exam
import com.spcode.tnpscprep.domain.ExamSelection
import com.spcode.tnpscprep.domain.RevisionLevel
import com.spcode.tnpscprep.ui.affairs.AffairsTab
import com.spcode.tnpscprep.ui.affairs.AffairsViewModel
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.ScreenTitle
import com.spcode.tnpscprep.ui.localized
import com.spcode.tnpscprep.ui.syllabus.LevelBar
import com.spcode.tnpscprep.ui.syllabus.SubjectUi
import com.spcode.tnpscprep.ui.syllabus.SyllabusScreen
import com.spcode.tnpscprep.ui.syllabus.SyllabusViewModel

/**
 * The Study tab is a HOME page: how far she is, what is due, and the subjects. Syllabus detail, Revise and News open as
 * pages with a Back arrow. Why: three equal tabs hid the answer to "where do I stand and what next?".
 * The open page and subject are saved (rememberSaveable), so rotation and tab switches keep them.
 */
@Composable
fun StudyScreen(syllabusVm: SyllabusViewModel, affairsVm: AffairsViewModel, headlinesVm: HeadlinesViewModel, todayVm: com.spcode.tnpscprep.ui.today.TodayViewModel, twoPane: Boolean) {
    var page by rememberSaveable { mutableStateOf("home") }
    var subject by rememberSaveable { mutableStateOf<String?>(null) }
    val home = { page = "home"; subject = null }
    BackHandler(enabled = page != "home", onBack = home)
    when (page) {
        "syllabus" -> if (twoPane) Column(Modifier.fillMaxSize()) { PageBar(R.string.nav_syllabus, home); SyllabusScreen(syllabusVm, true, subject) }
        else SyllabusScreen(syllabusVm, false, subject, onHome = home)
        "revise" -> Column(Modifier.fillMaxSize()) { PageBar(R.string.nav_revise, home); Box(Modifier.weight(1f)) { ReviseScreen(todayVm) } }
        "news" -> Column(Modifier.fillMaxSize()) { PageBar(R.string.nav_news, home); Box(Modifier.weight(1f)) { AffairsTab(affairsVm, headlinesVm) } }
        else -> StudyHome(syllabusVm, todayVm) { p, s -> subject = s; page = p }
    }
}

@Composable
private fun PageBar(title: Int, onBack: () -> Unit) {
    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
        ScreenTitle(stringResource(title))
    }
}

@Composable
private fun StudyHome(vm: SyllabusViewModel, todayVm: com.spcode.tnpscprep.ui.today.TodayViewModel, open: (String, String?) -> Unit) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val mask by vm.examMask.collectAsStateWithLifecycle()
    val last by vm.lastTouched.collectAsStateWithLifecycle()
    val ui by todayVm.ui.collectAsStateWithLifecycle()
    val levels = subjects.flatMap { s -> s.units.flatMap { u -> u.topics.map { it.level } } }
    val percent = RevisionLevel.percent(levels)
    val started = levels.count { it > 0 }
    val exams = if (mask == Exam.ALL_MASK) stringResource(R.string.my_exams_all) else ExamSelection.codes(mask).joinToString(" · ")
    val due = ui.progress.due.size
    val resume = last?.let { t -> subjects.firstOrNull { it.id == t.subjectId }?.let { t to it } }

    val sb = com.spcode.tnpscprep.ui.guide.rememberGuideBounds()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize(), state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.study_title)) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.fillMaxWidth().guideTarget(sb, "progress")) {
                Column(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFC8102E), Color(0xFF800020)))).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.study_progress_title, exams), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    Text(stringResource(R.string.percent_format, percent), style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f))) {
                        Box(Modifier.fillMaxWidth(started.toFloat() / levels.size.coerceAtLeast(1)).height(8.dp).background(Color.White))
                    }
                    Text(stringResource(R.string.study_started_fmt, started, levels.size), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    if (resume != null) {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { open("syllabus", resume.second.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF800020)),
                        ) { Text(stringResource(R.string.study_continue_fmt, localized(resume.first.nameEn, resume.first.nameTa)), maxLines = 1) }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().guideTarget(sb, "tiles"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LinkTile("🔁", stringResource(R.string.nav_revise), if (due > 0) stringResource(R.string.study_due_fmt, due) else stringResource(R.string.study_due_none), due > 0, Modifier.weight(1f)) { open("revise", null) }
                LinkTile("📰", stringResource(R.string.nav_news), stringResource(R.string.study_news_sub), false, Modifier.weight(1f)) { open("news", null) }
            }
        }
        item { OutlinedButton(onClick = { open("syllabus", null) }, modifier = Modifier.fillMaxWidth().guideTarget(sb, "search")) { Text("🔎  " + stringResource(R.string.syllabus_search_hint)) } }
        item { Text(stringResource(R.string.study_subjects), style = MaterialTheme.typography.titleMedium) }
        items(subjects.chunked(2), key = { it.first().id }) { row ->
            Row(Modifier.fillMaxWidth().let { m -> if (row.first().id == subjects.first().id) m.guideTarget(sb, "subjects") else m }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { s -> SubjectTile(s, Modifier.weight(1f)) { open("syllabus", s.id) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    com.spcode.tnpscprep.ui.guide.PageTour(
        "study",
        listOf(
            com.spcode.tnpscprep.ui.guide.CoachStep("progress", stringResource(R.string.guide_s1_title), stringResource(R.string.guide_s1_body), reveal = { listState.animateScrollToItem(1) }),
            com.spcode.tnpscprep.ui.guide.CoachStep("tiles", stringResource(R.string.guide_s2_title), stringResource(R.string.guide_s2_body), reveal = { listState.animateScrollToItem(2) }),
            com.spcode.tnpscprep.ui.guide.CoachStep("search", stringResource(R.string.guide_s3_title), stringResource(R.string.guide_s3_body), reveal = { listState.animateScrollToItem(3) }),
            com.spcode.tnpscprep.ui.guide.CoachStep("subjects", stringResource(R.string.guide_s4_title), stringResource(R.string.guide_s4_body), reveal = { listState.animateScrollToItem(5) }),
        ),
        sb, scrollable = listState,
    )
    }
}

@Composable
private fun LinkTile(icon: String, title: String, sub: String, highlight: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SubjectTile(s: SubjectUi, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(localized(s.nameEn, s.nameTa), style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2)
            LevelBar(s.levelCounts)
            Row {
                Text(stringResource(R.string.percent_format, s.percent), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.topic_count, s.topicCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
