package com.spcode.tnpscprep.domain

import com.spcode.tnpscprep.domain.Autosave.Action
import java.sql.DriverManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private val d = LocalDate.of(2026, 11, 17)
    private fun ms(day: LocalDate, h: Int, m: Int) = day.atTime(h, m).atZone(z).toInstant().toEpochMilli()
    private fun item(id: Long, date: LocalDate, mood: String? = null) = DiaryItem(id, date, mood, "x")

    @Test fun lateNightEntryCountsForPreviousDay() {
        assertEquals(d.minusDays(1), LogicalDay.of(ms(d, 0, 30), z))
        assertEquals(d.minusDays(1), LogicalDay.of(ms(d, 2, 59), z))
        assertEquals(d, LogicalDay.of(ms(d, 3, 0), z))
    }

    @Test fun autosaveWaitsForThePause() {
        assertEquals(Action.NONE, Autosave.decide("abc", "ab", false, 799, leaving = false))
        assertEquals(Action.SAVE, Autosave.decide("abc", "ab", false, 800, leaving = false))
    }

    @Test fun autosaveSkipsUnchangedAndFlushesOnLeave() {
        assertEquals(Action.NONE, Autosave.decide("same", "same", false, 5000, false))
        assertEquals(Action.SAVE, Autosave.decide("abc", "ab", false, 10, leaving = true)) // leaving: no waiting
    }

    @Test fun blankHandling() {
        assertEquals(Action.NONE, Autosave.decide("  ", null, true, 5000, true))   // blank NEW entry is never stored
        assertEquals(Action.NONE, Autosave.decide("", "old", false, 5000, false))  // cleared mid-typing: do not delete yet
        assertEquals(Action.DELETE, Autosave.decide("", "old", false, 0, true))    // cleared and leaving: delete
    }

    @Test fun groupsByMonthKeepingOrder() {
        val g = DiaryTimeline.groupByMonth(listOf(item(1, LocalDate.of(2026, 11, 20)), item(2, LocalDate.of(2026, 11, 2)), item(3, LocalDate.of(2026, 10, 30)), item(4, LocalDate.of(2025, 11, 1))))
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10), YearMonth.of(2025, 11)), g.map { it.month })
        assertEquals(listOf(1L, 2L), g[0].items.map { it.id })
    }

    @Test fun likePatternEscapesAndNormalises() {
        assertNull(DiarySearch.likePattern("   "))
        assertEquals("%100\\% sure\\_ok%", DiarySearch.likePattern(" 100% sure_ok "))
        assertEquals("%a\\\\b%", DiarySearch.likePattern("a\\b"))
        // two ways to type the same Tamil letter (precomposed vs base + sign) become one form
        assertEquals(DiarySearch.likePattern("ொ"), DiarySearch.likePattern("ொ"))
    }

    @Test fun likeOnRealSqliteFindsTamilSubstrings() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { c ->
            c.createStatement().use { it.execute("CREATE TABLE t(id INTEGER, text TEXT)") }
            val rows = listOf(1 to "இன்று நான் வரலாறு படித்தேன்", 2 to "Studied polity for 100% of the morning", 3 to "snake_case and plain text", 4 to "வணக்கம் உலகம்")
            c.prepareStatement("INSERT INTO t VALUES(?,?)").use { st -> rows.forEach { (i, t) -> st.setInt(1, i); st.setString(2, t); st.executeUpdate() } }
            fun find(q: String): List<Int> = c.prepareStatement("SELECT id FROM t WHERE text LIKE ? ESCAPE '\\' ORDER BY id").use { st ->
                st.setString(1, DiarySearch.likePattern(q)); st.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getInt(1)) } }
            }
            assertEquals(listOf(1), find("வரலாறு"))         // a whole Tamil word
            assertEquals(listOf(1), find("படித்"))           // a mid-word fragment: substring search works, token search would not
            assertEquals(listOf(4), find("உலகம்"))
            assertEquals(listOf(2), find("100%"))            // a literal percent sign, not a wildcard
            assertEquals(listOf(3), find("snake_case"))      // a literal underscore
            assertEquals(listOf(2), find("POLITY"))          // case-insensitive for Latin
            assertTrue(find("zzz").isEmpty())
        }
    }

    @Test fun promptRotatesDailyAndStaysInRange() {
        assertEquals(DiaryPrompts.indexFor(d, 3), DiaryPrompts.indexFor(d, 3))
        (0 until 400).forEach { assertTrue(DiaryPrompts.indexFor(d.plusDays(it.toLong()), 3) in 0..2) }
        assertEquals(0, DiaryPrompts.indexFor(d, 0))
    }

    @Test fun weeklyReviewCountsOnly() {
        val items = listOf(item(1, d, "CHEER"), item(2, d.minusDays(1), "CHEER"), item(3, d.minusDays(2), "WORRIED") /* Sunday = last week */, item(4, d, null))
        val w = DiaryReview.week(items, d, mapOf(d to 40, d.minusDays(1) to 20, d.minusDays(5) to 99))
        assertEquals(3, w.entries); assertEquals(mapOf("CHEER" to 2), w.moods); assertEquals(60, w.studyMinutes)
    }
}
