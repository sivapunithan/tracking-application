package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodCheckinTest {
    private val today = LocalDate.of(2026, 11, 17)
    private fun row(d: LocalDate, v: String) = MoodRow(d.toString(), "{\"value\":\"$v\"}")

    @Test fun latestRowOfADayWinsAndBadRowsAreSkipped() {
        val m = MoodCheckins.parse(listOf(row(today, "PROUD"), row(today, "CHEER"), MoodRow("not-a-date", "{}"), row(today.minusDays(1), "NOPE"), MoodRow(today.minusDays(2).toString(), "garbage")))
        assertEquals(mapOf(today to Mood.CHEER), m)
    }
    @Test fun lastDaysAreOldestFirstEndingToday() {
        val d = MoodCheckins.lastDays(today, 3)
        assertEquals(listOf(today.minusDays(2), today.minusDays(1), today), d)
    }
    @Test fun countsAreInTheWindowMostFrequentFirst() {
        val map = mapOf(today to Mood.PROUD, today.minusDays(1) to Mood.PROUD, today.minusDays(2) to Mood.CHEER, today.minusDays(3) to Mood.WORRIED, today.minusDays(40) to Mood.CRY)
        assertEquals(listOf(Mood.PROUD to 2, Mood.CHEER to 1, Mood.WORRIED to 1), MoodCheckins.counts(map, today, 30)) // CRY is 40 days ago: outside; CHEER before WORRIED (enum order)
    }
    @Test fun emptyGivesNothing() = assertEquals(emptyList<Pair<Mood, Int>>(), MoodCheckins.counts(emptyMap(), today))
}
