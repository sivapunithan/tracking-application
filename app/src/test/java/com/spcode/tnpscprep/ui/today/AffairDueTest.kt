package com.spcode.tnpscprep.ui.today

import com.spcode.tnpscprep.domain.AffairCategory
import com.spcode.tnpscprep.ui.affairs.AffairUi
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AffairDueTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 17)
    private fun ms(daysAgo: Long) = today.minusDays(daysAgo).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private fun affair(id: String, level: Int, daysAgo: Long, bookmarked: Boolean = false, user: Boolean = false) = AffairUi(
        id = id, date = today.minusDays(30), category = AffairCategory.TN, titleEn = id, titleTa = id, summaryEn = "s", summaryTa = "s",
        sourceName = "", url = "", verified = false, isUser = user, userId = null, bookmarked = bookmarked, level = level, lastUpdated = ms(daysAgo),
    )

    @Test fun a_bookmark_alone_is_never_due() {
        assertTrue(buildAffairDue(listOf(affair("a", level = 0, daysAgo = 100, bookmarked = true)), today, zone).isEmpty())
    }

    @Test fun revised_items_are_due_after_their_interval() {
        val due = buildAffairDue(listOf(affair("a", 1, 3), affair("b", 1, 2), affair("c", 2, 7)), today, zone)
        assertEquals(setOf("a", "c"), due.map { it.topicId }.toSet())
        assertTrue(due.all { it.affairCategory == AffairCategory.TN })
    }

    @Test fun own_entries_follow_the_same_schedule() {
        val due = buildAffairDue(listOf(affair("user-3", 1, 10, user = true)), today, zone)
        assertEquals("user-3", due.single().topicId)
        assertEquals(7, due.single().daysOverdue)
    }

    @Test fun merge_orders_topics_and_affairs_together_by_how_overdue_they_are() {
        val topic = DueItem("t1", "T", "T", "S", "S", level = 1, daysOverdue = 2)
        val merged = mergeDue(listOf(topic), buildAffairDue(listOf(affair("a", 1, 9)), today, zone)) // a: 9d ago, due after 3 -> 6 overdue
        assertEquals(listOf("a", "t1"), merged.map { it.topicId })
    }
}
