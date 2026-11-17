package com.spcode.tnpscprep.ui.syllabus

import com.spcode.tnpscprep.data.db.SubjectEntity
import com.spcode.tnpscprep.data.db.SyllabusUnitEntity
import com.spcode.tnpscprep.data.db.TopicWithLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusUiTest {
    private val subjects = listOf(SubjectEntity("s1", "S1", "S1", 0), SubjectEntity("s2", "S2", "S2", 1))
    private val units = listOf(
        SyllabusUnitEntity("u1", "s1", "U1", "U1", 0),
        SyllabusUnitEntity("u2", "s1", "U2", "U2", 1),
        SyllabusUnitEntity("u3", "s2", "U3", "U3", 0),
    )
    private fun topic(id: String, unit: String, subject: String, level: Int, exams: Int = 7) =
        TopicWithLevel(id, subject, unit, id, id, exams, level)

    @Test fun percent_is_per_subject_from_levels() {
        val ui = buildSyllabusUi(subjects, units, listOf(
            topic("a", "u1", "s1", 4), topic("b", "u2", "s1", 0), topic("c", "u3", "s2", 2),
        ))
        assertEquals(listOf(50, 50), ui.map { it.percent })
        assertEquals(2, ui[0].topicCount)
    }

    @Test fun subjects_and_units_with_no_topics_under_the_filter_are_hidden() {
        val ui = buildSyllabusUi(subjects, units, listOf(topic("a", "u1", "s1", 1)))
        assertEquals(listOf("s1"), ui.map { it.id })
        assertEquals(listOf("u1"), ui[0].units.map { it.id }) // u2 is empty -> hidden
    }

    @Test fun order_follows_syllabus_order_and_topic_order_is_preserved() {
        val ui = buildSyllabusUi(subjects, units, listOf(
            topic("z", "u1", "s1", 0), topic("a", "u1", "s1", 0), topic("m", "u3", "s2", 0),
        ))
        assertEquals(listOf("s1", "s2"), ui.map { it.id })
        assertEquals(listOf("z", "a"), ui[0].units[0].topics.map { it.id })
    }

    @Test fun nothing_to_show_gives_empty_list() =
        assertTrue(buildSyllabusUi(subjects, units, emptyList()).isEmpty())

    @Test fun exam_filter_masks_match_the_exam_bits() {
        assertEquals(1, ExamFilter.G1.mask)
        assertEquals(2, ExamFilter.G2.mask)
        assertEquals(4, ExamFilter.G4.mask)
        assertEquals(7, ExamFilter.ALL.mask)
    }
}
