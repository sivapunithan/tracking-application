package com.spcode.tnpscprep.ui.syllabus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.domain.Exam
import com.spcode.tnpscprep.domain.ExamSelection

/** "My exams": tick one or more of G1 / G2 / G4. Unticking the last one means all exams (see [ExamSelection.toggle]). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamPicker(mask: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Exam.entries.forEach { e ->
            FilterChip(selected = ExamSelection.has(mask, e), onClick = { onChange(ExamSelection.toggle(mask, e)) }, label = { Text(e.code) })
        }
    }
}
