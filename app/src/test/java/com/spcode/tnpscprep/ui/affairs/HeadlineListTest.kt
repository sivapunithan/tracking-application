package com.spcode.tnpscprep.ui.affairs

import com.spcode.tnpscprep.data.db.AffairStateEntity
import com.spcode.tnpscprep.data.db.NewsItemEntity
import com.spcode.tnpscprep.domain.AffairCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadlineListTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun ms(date: String) = LocalDate.parse(date).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun n(id: String, date: String, cat: String = "INDIA") =
        NewsItemEntity(id, "f", "The Hindu", "Headline $id", "https://e.org/$id", ms(date), cat, "en", ms(date))

    private val news = listOf(n("a", "2026-11-10", "TN"), n("b", "2026-11-16", "POLITY"), n("c", "2026-11-12", "TN"))

    @Test fun newest_first() =
        assertEquals(listOf("news-b", "news-c", "news-a"), buildHeadlineList(news, emptyList(), HeadlineFilter(), zone).map { it.id })

    @Test fun category_filter() =
        assertEquals(listOf("news-c", "news-a"), buildHeadlineList(news, emptyList(), HeadlineFilter(category = AffairCategory.TN), zone).map { it.id })

    @Test fun starred_only_uses_the_shared_state_table_with_a_news_prefix() {
        val states = listOf(AffairStateEntity("news-a", true, 0, 0L))
        assertEquals(listOf("news-a"), buildHeadlineList(news, states, HeadlineFilter(starredOnly = true), zone).map { it.id })
    }

    @Test fun news_items_carry_state_flags_source_and_link() {
        val ui = newsToUi(n("a", "2026-11-10"), AffairStateEntity("news-a", true, 2, 99L), zone)
        assertTrue(ui.isNews && !ui.verified && ui.bookmarked)
        assertEquals(2, ui.level); assertEquals(99L, ui.lastUpdated)
        assertEquals("The Hindu", ui.sourceName); assertEquals("https://e.org/a", ui.url)
        assertEquals(LocalDate.of(2026, 11, 10), ui.date)
    }

    @Test fun the_date_uses_the_users_time_zone() {
        val late = NewsItemEntity("z", "f", "s", "t", "https://e.org/z", Instant.parse("2026-11-16T20:00:00Z").toEpochMilli(), "INDIA", "en", 0)
        assertEquals(LocalDate.of(2026, 11, 17), newsToUi(late, null, ZoneId.of("Asia/Kolkata")).date) // 01:30 next day in IST
    }

    @Test fun range_days() {
        assertEquals(listOf(7, 30, null), HeadlineRange.entries.map { it.days })
    }
}
