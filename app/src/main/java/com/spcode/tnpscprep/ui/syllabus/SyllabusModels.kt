package com.spcode.tnpscprep.ui.syllabus

import com.spcode.tnpscprep.data.db.SubjectEntity
import com.spcode.tnpscprep.data.db.SyllabusUnitEntity
import com.spcode.tnpscprep.data.db.TopicWithLevel
import com.spcode.tnpscprep.domain.Exam
import com.spcode.tnpscprep.domain.RevisionLevel

enum class ExamFilter(val mask: Int) {
    ALL(Exam.ALL_MASK), G1(Exam.G1.bit), G2(Exam.G2.bit), G4(Exam.G4.bit)
}

data class TopicUi(
    val id: String, val nameEn: String, val nameTa: String, val exams: Int, val level: Int,
    val starred: Boolean = false, val hasNote: Boolean = false,
)
data class UnitUi(val id: String, val nameEn: String, val nameTa: String, val topics: List<TopicUi>)
data class SubjectUi(
    val id: String, val nameEn: String, val nameTa: String,
    val percent: Int, val topicCount: Int, val units: List<UnitUi>,
)

/**
 * Pure function (no Android): easy to unit test. [topics] are ALREADY filtered by exam (SQL does
 * that), so a subject's percent reflects only the selected exam. Subjects/units with nothing
 * under the current filter are hidden.
 */
fun buildSyllabusUi(
    subjects: List<SubjectEntity>,
    units: List<SyllabusUnitEntity>,
    topics: List<TopicWithLevel>,
): List<SubjectUi> {
    val topicsByUnit = topics.groupBy { it.unitId }
    val unitsBySubject = units.groupBy { it.subjectId }
    return subjects.mapNotNull { s ->
        val unitUis = unitsBySubject[s.id].orEmpty().mapNotNull { u ->
            val ts = topicsByUnit[u.id].orEmpty()
            if (ts.isEmpty()) null
            else UnitUi(u.id, u.nameEn, u.nameTa, ts.map { TopicUi(it.id, it.nameEn, it.nameTa, it.exams, it.level, it.starred, it.hasNote) })
        }
        if (unitUis.isEmpty()) null
        else {
            val all = unitUis.flatMap { it.topics }
            SubjectUi(s.id, s.nameEn, s.nameTa, RevisionLevel.percent(all.map { it.level }), all.size, unitUis)
        }
    }
}
