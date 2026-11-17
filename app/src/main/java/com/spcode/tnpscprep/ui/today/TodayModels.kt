package com.spcode.tnpscprep.ui.today

import com.spcode.tnpscprep.data.db.StarredTopic
import com.spcode.tnpscprep.data.db.StudiedTopic
import com.spcode.tnpscprep.data.db.SubjectEntity
import com.spcode.tnpscprep.data.db.TopicWithLevel
import com.spcode.tnpscprep.domain.RevisionLevel
import com.spcode.tnpscprep.domain.RevisionSchedule
import java.time.LocalDate
import java.time.ZoneId

data class DueItem(
    val topicId: String,
    val nameEn: String, val nameTa: String,
    val subjectEn: String, val subjectTa: String,
    val level: Int,
    /** 0 = due today; positive = days overdue. */
    val daysOverdue: Int,
)

data class SubjectProgress(
    val subjectId: String, val nameEn: String, val nameTa: String,
    val percent: Int, val topicCount: Int,
)

data class StarredItem(
    val topicId: String,
    val nameEn: String, val nameTa: String,
    val subjectEn: String, val subjectTa: String,
    val level: Int,
    val note: String,
)

data class TodayProgress(
    val readinessPercent: Int,
    val totalTopics: Int,
    val notStarted: Int,
    val due: List<DueItem>,
    val subjects: List<SubjectProgress> = emptyList(),
    val starred: List<StarredItem> = emptyList(),
)

/**
 * Pure function (no Android). [allTopics] and [studied] are already limited to the chosen exam.
 * The most overdue topics come first; ties go to the lower level (less-secure knowledge).
 */
fun buildTodayProgress(
    studied: List<StudiedTopic>,
    allTopics: List<TopicWithLevel>,
    subjects: List<SubjectEntity>,
    today: LocalDate,
    zone: ZoneId,
    starred: List<StarredTopic> = emptyList(),
): TodayProgress {
    val subjectById = subjects.associateBy { it.id }
    val due = studied.mapNotNull { s ->
        val dueDate = RevisionSchedule.dueDate(s.level, s.lastUpdated, zone) ?: return@mapNotNull null
        val overdue = RevisionSchedule.daysOverdue(dueDate, today)
        if (overdue < 0) return@mapNotNull null
        val subj = subjectById[s.subjectId]
        DueItem(s.topicId, s.nameEn, s.nameTa, subj?.nameEn.orEmpty(), subj?.nameTa.orEmpty(), s.level, overdue.toInt())
    }.sortedWith(compareByDescending<DueItem> { it.daysOverdue }.thenBy { it.level }.thenBy { it.nameEn })

    return TodayProgress(
        readinessPercent = RevisionLevel.percent(allTopics.map { it.level }),
        totalTopics = allTopics.size,
        notStarted = allTopics.count { it.level == RevisionLevel.NOT_STARTED },
        due = due,
        subjects = subjects.mapNotNull { s ->
            val ts = allTopics.filter { it.subjectId == s.id }
            if (ts.isEmpty()) null else SubjectProgress(s.id, s.nameEn, s.nameTa, RevisionLevel.percent(ts.map { it.level }), ts.size)
        },
        starred = starred.map {
            val subj = subjectById[it.subjectId]
            StarredItem(it.topicId, it.nameEn, it.nameTa, subj?.nameEn.orEmpty(), subj?.nameTa.orEmpty(), it.level, it.note)
        },
    )
}
