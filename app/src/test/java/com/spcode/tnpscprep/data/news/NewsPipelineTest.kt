package com.spcode.tnpscprep.data.news

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsPipelineTest {
    private val now = Instant.parse("2026-11-17T10:00:00Z").toEpochMilli()
    private val feed = FeedConfig("f", "Test Feed", "https://e.org/rss", "en", "INDIA", minScore = 2, maxItems = 60)
    private fun raw(title: String, link: String, ageDays: Long = 1, published: Boolean = true) =
        RawFeedItem(title, link, if (published) Instant.ofEpochMilli(now).minus(Duration.ofDays(ageDays)) else null)

    @Test fun keeps_relevant_items_and_drops_the_rest() {
        val out = NewsPipeline.toEntities(feed, listOf(
            raw("Supreme Court rules on Bill", "https://e.org/1"),
            raw("Celebrity wedding photos", "https://e.org/2"),
        ), now)
        assertEquals(listOf("https://e.org/1"), out.map { it.url })
        assertEquals("POLITY", out.single().category)
    }

    @Test fun tracking_parameters_and_fragments_are_removed_so_duplicates_collapse() {
        val a = NewsPipeline.normalizeUrl("https://E.org/a?utm_source=x&id=7#top")
        val b = NewsPipeline.normalizeUrl("https://e.org/a?id=7&fbclid=zzz")
        assertEquals("https://e.org/a?id=7", a)
        assertEquals(a, b)
        val out = NewsPipeline.toEntities(feed, listOf(raw("Parliament passes Bill", a!!), raw("Parliament passes Bill", b!!)), now)
        assertEquals(1, out.size)
    }

    @Test fun ids_are_stable_and_differ_per_url() {
        assertEquals(NewsPipeline.idFor("https://e.org/a"), NewsPipeline.idFor("https://e.org/a"))
        assertNotEquals(NewsPipeline.idFor("https://e.org/a"), NewsPipeline.idFor("https://e.org/b"))
        assertEquals(16, NewsPipeline.idFor("https://e.org/a").length)
    }

    @Test fun non_https_links_are_dropped() {
        assertNull(NewsPipeline.normalizeUrl("http://e.org/a"))
        assertNull(NewsPipeline.normalizeUrl("javascript:alert(1)"))
        assertTrue(NewsPipeline.toEntities(feed, listOf(raw("Supreme Court Bill", "http://e.org/a")), now).isEmpty())
    }

    @Test fun items_older_than_the_retention_window_are_dropped() {
        val out = NewsPipeline.toEntities(feed, listOf(raw("Supreme Court Bill", "https://e.org/old", ageDays = 200)), now)
        assertTrue(out.isEmpty())
    }

    @Test fun future_dates_are_clamped_to_now_and_missing_dates_mean_first_seen_now() {
        val future = RawFeedItem("Supreme Court Bill", "https://e.org/f", Instant.ofEpochMilli(now).plus(Duration.ofDays(30)))
        val none = raw("Parliament Bill passes", "https://e.org/n", published = false)
        val out = NewsPipeline.toEntities(feed, listOf(future, none), now)
        assertTrue(out.all { it.publishedAt == now })
    }

    @Test fun titles_are_cleaned_and_length_limited() {
        val out = NewsPipeline.toEntities(feed, listOf(raw("<b>Supreme   Court</b>\n Bill " + "x".repeat(500), "https://e.org/t")), now)
        val t = out.single().title
        assertTrue(!t.contains("<") && !t.contains("  ") && t.length <= 220)
    }

    @Test fun newest_first_and_capped_per_feed() {
        val items = (1..10).map { raw("Supreme Court Bill $it", "https://e.org/$it", ageDays = it.toLong()) }
        val out = NewsPipeline.toEntities(feed.copy(maxItems = 3), items, now)
        assertEquals(listOf("https://e.org/1", "https://e.org/2", "https://e.org/3"), out.map { it.url })
    }

    @Test fun feeds_json_is_valid() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val text = java.io.File("src/main/assets/feeds.json").readText()
        val feeds = json.decodeFromString<FeedListDto>(text).feeds
        assertTrue(feeds.size >= 5)
        assertEquals(feeds.size, feeds.map { it.id }.toSet().size)
        val hosts = feeds.map { FeedPolicy.hostOf(it.url) }.toSet()
        assertTrue("every feed url must be https", hosts.none { it == null })
        assertTrue(feeds.all { com.spcode.tnpscprep.domain.AffairCategory.fromName(it.categoryHint) != null })
    }
}
