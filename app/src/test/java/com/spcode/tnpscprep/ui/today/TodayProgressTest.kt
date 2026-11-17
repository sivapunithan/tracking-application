package com.spcode.tnpscprep.ui.today

import com.spcode.tnpscprep.data.db.StudiedTopic
import com.spcode.tnpscprep.data.db.SubjectEntity
import com.spcode.tnpscprep.data.db.TopicWithLevel
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayProgressTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 17)
    private val subjects = listOf(SubjectEntity("s1", "Polity", "ஆட்சியியல்", 0))

    private fun ms(daysAgo: Long) = today.minusDays(daysAgo).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun studied(id: String, level: Int, daysAgo: Long) = StudiedTopic(id, "s1", id, id, level, ms(daysAgo))
    private fun topic(id: String, level: Int) = TopicWithLevel(id, "s1", "u1", id, id, 7, level)

    @Test fun only_topics_past_their_interval_are_due() {
        val p = buildTodayProgress(
            studied = listOf(studied("a", 1, 3), studied("b", 1, 2), studied("c", 2, 7), studied("d", 2, 6)),
            allTopics = emptyList(), subjects = subjects, today = today, zone = zone,
        )
        assertEquals(setOf("a", "c"), p.due.map { it.topicId }.toSet()) // a: 3d/3d, c: 7d/7d; b and d not yet
    }

    @Test fun most_overdue_first_then_lower_level() {
        val p = buildTodayProgress(
            studied = listOf(studied("late", 1, 10), studied("lowlvl", 1, 3), studied("highlvl", 2, 7)),
            allTopics = emptyList(), subjects = subjects, today = today, zone = zone,
        )
        assertEquals(listOf("late", "lowlvl", "highlvl"), p.due.map { it.topicId })
        assertEquals(7, p.due.first().daysOverdue) // 10 days ago, due after 3 -> 7 overdue
        assertEquals(0, p.due.last().daysOverdue)
    }

    @Test fun subject_names_are_attached() {
        val p = buildTodayProgress(listOf(studied("a", 1, 5)), emptyList(), subjects, today, zone)
        assertEquals("Polity", p.due.single().subjectEn)
        assertEquals("ஆட்சியியல்", p.due.single().subjectTa)
    }

    @Test fun readiness_and_not_started_come_from_all_topics() {
        val p = buildTodayProgress(
            studied = emptyList(),
            allTopics = listOf(topic("a", 4), topic("b", 0), topic("c", 0), topic("d", 0)),
            subjects = subjects, today = today, zone = zone,
        )
        assertEquals(25, p.readinessPercent)
        assertEquals(4, p.totalTopics)
        assertEquals(3, p.notStarted)
        assertTrue(p.due.isEmpty())
    }

    @Test fun nothing_studied_means_nothing_due() {
        val p = buildTodayProgress(emptyList(), emptyList(), subjects, today, zone)
        assertEquals(0, p.readinessPercent)
        assertTrue(p.due.isEmpty())
    }
}
