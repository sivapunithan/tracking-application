package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryStatsTest {
    private val today = LocalDate.of(2026, 11, 18)
    private fun d(vararg back: Int) = back.map { today.minusDays(it.toLong()) }.toSet()
    @Test fun streakCountsBackFromToday() = assertEquals(3, DiaryStats.streak(d(0, 1, 2, 4), today))
    @Test fun unwrittenTodayDoesNotBreakIt() = assertEquals(2, DiaryStats.streak(d(1, 2, 5), today))
    @Test fun gapBreaksIt() = assertEquals(0, DiaryStats.streak(d(2, 3), today))
    @Test fun emptyIsZero() = assertEquals(0, DiaryStats.streak(emptySet(), today))
    @Test fun wordsCount() { assertEquals(3, DiaryStats.words(" a  bb\nccc ")); assertEquals(0, DiaryStats.words("  ")); assertEquals(2, DiaryStats.words("இன்று நல்லது")) }
}
