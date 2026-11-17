package com.spcode.tnpscprep.ui.syllabus

import com.spcode.tnpscprep.data.db.SubjectEntity
import com.spcode.tnpscprep.data.db.SyllabusUnitEntity
import com.spcode.tnpscprep.data.db.TopicWithLevel
import com.spcode.tnpscprep.domain.RevisionLevel

data class TopicUi(
    val id: String, val nameEn: String, val nameTa: String, val exams: Int, val level: Int,
    val starred: Boolean = false, val hasNote: Boolean = false,
)
data class UnitUi(val id: String, val nameEn: String, val nameTa: String, val topics: List<TopicUi>)
data class SubjectUi(
    val id: String, val nameEn: String, val nameTa: String,
    val percent: Int, val topicCount: Int, val units: List<UnitUi>,
    /** How many topics sit at each revision level 0..MAX (index = level): drives the stacked bar. */
    val levelCounts: List<Int> = emptyList(),
)
/** A topic found by the search box, with the subject it lives in. */
data class TopicHit(val subjectId: String, val subjectNameEn: String, val subjectNameTa: String, val topic: TopicUi)

/** Pure search over the already exam-filtered syllabus: case-insensitive substring on the topic or subject name, English OR Tamil (works for any script). */
fun searchTopics(subjects: List<SubjectUi>, query: String, limit: Int = 60): List<TopicHit> {
    val q = java.text.Normalizer.normalize(query.trim(), java.text.Normalizer.Form.NFC).lowercase()
    if (q.isEmpty()) return emptyList()
    val out = ArrayList<TopicHit>()
    fun norm(x: String) = java.text.Normalizer.normalize(x, java.text.Normalizer.Form.NFC).lowercase()
    for (s in subjects) for (u in s.units) for (t in u.topics) {
        // a topic matches by its own name OR by its subject's name (searching "polity" lists the Polity topics)
        if (listOf(t.nameEn, t.nameTa, s.nameEn, s.nameTa).any { norm(it).contains(q) }) { out += TopicHit(s.id, s.nameEn, s.nameTa, t); if (out.size >= limit) return out }
    }
    return out
}

/** Counts of topics per level 0..MAX. */
fun levelCounts(levels: List<Int>): List<Int> {
    val c = IntArray(RevisionLevel.MAX + 1)
    levels.forEach { c[RevisionLevel.clamp(it)]++ }
    return c.toList()
}

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
            SubjectUi(s.id, s.nameEn, s.nameTa, RevisionLevel.percent(all.map { it.level }), all.size, unitUis, levelCounts(all.map { it.level }))
        }
    }
}
