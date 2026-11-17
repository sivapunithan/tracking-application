package com.spcode.tnpscprep.ui.syllabus

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {
    private fun t(id: String, en: String, ta: String) = TopicUi(id, en, ta, 7, 0)
    private val subjects = listOf(SubjectUi("s1", "Polity", "அரசியல்", 0, 2, listOf(UnitUi("u1", "U", "U", listOf(t("a", "Fundamental Rights", "அடிப்படை உரிமைகள்"), t("b", "Preamble", "முகவுரை")))), listOf(2, 0, 0, 0, 0)))
    @Test fun matchesEnglishCaseInsensitive() = assertEquals(listOf("a"), searchTopics(subjects, "rights").map { it.topic.id })
    @Test fun matchesTamil() = assertEquals(listOf("b"), searchTopics(subjects, "முகவு").map { it.topic.id })
    @Test fun matchesSubjectName() = assertEquals(listOf("a", "b"), searchTopics(subjects, "polity").map { it.topic.id })
    @Test fun blankGivesNothing() = assertEquals(0, searchTopics(subjects, "  ").size)
    @Test fun levelCountsSum() = assertEquals(listOf(1, 1, 0, 0, 1), levelCounts(listOf(0, 1, 4)))
}
