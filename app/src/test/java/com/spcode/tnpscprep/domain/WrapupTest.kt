package com.spcode.tnpscprep.domain

import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WrapupTest {
    private val h = WrapupHeadings("What went well", "What was hard", "Tomorrow")
    private val day = LocalDate.of(2026, 11, 17)

    @Test fun renderSkipsEmptySectionsAndNumbersTomorrow() {
        val c = WrapupContent(well = " Finished polity ", hard = "", tomorrow = listOf("Read Preamble", " ", "Review news"))
        assertEquals("What went well\nFinished polity\n\nTomorrow\n1. Read Preamble\n2. Review news", Wrapup.render(c, h))
        assertEquals("", Wrapup.render(WrapupContent(), h))
    }

    @Test fun jsonRoundTripAndBadInput() {
        val c = WrapupContent(well = "இன்று நல்லது", hard = "x", intensity = 3, tomorrow = listOf("a"))
        assertEquals(c, Wrapup.decode(Wrapup.encode(c)))
        assertNull(Wrapup.decode("not json")); assertNull(Wrapup.decode(null)); assertNull(Wrapup.decode(""))
    }

    @Test fun draftRestoresOnlyForItsOwnDay() {
        val d = WrapupDraft(day.toString(), step = 3, mood = "PROUD", content = WrapupContent(well = "half"))
        val s = WrapupDrafts.encode(d)
        assertEquals(d, WrapupDrafts.decodeFor(day, s))
        assertNull(WrapupDrafts.decodeFor(day.plusDays(1), s)) // stale draft from another day
        assertNull(WrapupDrafts.decodeFor(day, "{broken"))
    }

    private class FakeStore : WrapupStore {
        val rows = ArrayList<WrapupRow>(); var nextId = 1L
        override suspend fun find(date: LocalDate) = rows.firstOrNull { it.date == date }
        override suspend fun insert(date: LocalDate, text: String, mood: String?, json: String, now: Long): Long { rows += WrapupRow(nextId, date, text, mood, json); return nextId++ }
        override suspend fun update(id: Long, text: String, mood: String?, json: String, now: Long) {
            val i = rows.indexOfFirst { it.id == id }; rows[i] = rows[i].copy(text = text, mood = mood, json = json)
        }
    }

    @Test fun savingTwiceEditsTheSameEntry() = runBlocking {
        val s = FakeStore()
        val id1 = WrapupSaver.save(s, day, WrapupContent(well = "one"), "PROUD", h, 1)
        val id2 = WrapupSaver.save(s, day, WrapupContent(well = "two"), "CHEER", h, 2)
        assertEquals(id1, id2); assertEquals(1, s.rows.size)
        assertEquals("CHEER", s.rows[0].mood); assertTrue(s.rows[0].text.contains("two"))
        WrapupSaver.save(s, day.plusDays(1), WrapupContent(well = "next day"), null, h, 3)
        assertEquals(2, s.rows.size) // another day = another wrap-up
    }

    @Test fun planOrderMoves() {
        assertEquals(listOf("b", "c", "a"), PlanOrder.move(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("c", "a", "b"), PlanOrder.move(listOf("a", "b", "c"), 2, 0))
        assertEquals(listOf("a", "b"), PlanOrder.move(listOf("a", "b"), 0, 5)) // out of range: unchanged
    }
}

class ReminderTest {
    private val z = java.time.ZoneId.of("Asia/Kolkata")
    private fun ms(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(z).toInstant().toEpochMilli()
    private val day = LocalDate.of(2026, 11, 17)

    @Test fun consistencyCountsDaysInTheWindowOnly() {
        val dates = setOf(day, day.minusDays(1), day.minusDays(3), day.minusDays(6), day.minusDays(7), day.minusDays(20))
        assertEquals(4, WrapupConsistency.lastDays(dates, day, 7)) // today, -1, -3, -6 (the -7 day is outside)
        assertEquals(0, WrapupConsistency.lastDays(emptySet(), day))
    }
    @Test fun nextTriggerIsTodayIfStillAhead() = assertEquals(ms(day, 21), ReminderTime.nextTrigger(ms(day, 20, 59), 21 * 60, z))
    @Test fun nextTriggerIsTomorrowIfPassedOrExactlyNow() {
        assertEquals(ms(day.plusDays(1), 21), ReminderTime.nextTrigger(ms(day, 21, 1), 21 * 60, z))
        assertEquals(ms(day.plusDays(1), 21), ReminderTime.nextTrigger(ms(day, 21), 21 * 60, z))
    }
    @Test fun lateNightTimeWorks() = assertEquals(ms(day.plusDays(1), 0, 30), ReminderTime.nextTrigger(ms(day, 23, 50), 30, z))
    @Test fun clampWrapsMinutes() { assertEquals(23 * 60 + 45, ReminderTime.clamp(-15)); assertEquals(0, ReminderTime.clamp(1440)) }
    @Test fun notifyOnlyWhenOnAndNotWrapped() {
        assertTrue(ReminderTime.shouldNotify(true, false))
        assertTrue(!ReminderTime.shouldNotify(true, true))
        assertTrue(!ReminderTime.shouldNotify(false, false))
    }
}
