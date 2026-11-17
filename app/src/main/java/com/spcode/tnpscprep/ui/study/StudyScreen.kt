package com.spcode.tnpscprep.ui.study

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.ui.affairs.AffairsTab
import com.spcode.tnpscprep.ui.affairs.AffairsViewModel
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.syllabus.SyllabusScreen
import com.spcode.tnpscprep.ui.syllabus.SyllabusViewModel

/**
 * The Study tab: one place for everything she reads. A segmented control switches between the syllabus and the news
 * (Revise holds the due, progress and starred lists moved from Today). The chosen segment is saved, so it survives rotation and tab switches.
 */
@Composable
fun StudyScreen(syllabusVm: SyllabusViewModel, affairsVm: AffairsViewModel, headlinesVm: HeadlinesViewModel, todayVm: com.spcode.tnpscprep.ui.today.TodayViewModel, twoPane: Boolean) {
    var segment by rememberSaveable { mutableIntStateOf(0) }
    val labels = listOf(R.string.nav_syllabus, R.string.nav_revise, R.string.nav_news)
    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
            labels.forEachIndexed { i, res ->
                SegmentedButton(selected = segment == i, onClick = { segment = i }, shape = SegmentedButtonDefaults.itemShape(i, labels.size)) {
                    Text(stringResource(res))
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (segment) {
                0 -> SyllabusScreen(syllabusVm, twoPane)
                1 -> ReviseScreen(todayVm)
                else -> AffairsTab(affairsVm, headlinesVm)
            }
        }
    }
}
