package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DiaryMemoriesTest {
    private val today = LocalDate.of(2026, 11, 17)
    private fun item(id: Long, d: LocalDate) = DiaryItem(id, d, null, "x")

    @Test fun findsSameDateInEarlierYears() {
        val items = listOf(item(3, today), item(2, today.minusYears(1)), item(1, today.minusYears(3)), item(9, today.minusYears(1).minusDays(1)))
        val m = DiaryMemories.onThisDay(items, today)
        assertEquals(listOf(1 to 2L, 3 to 1L), m.map { it.yearsAgo to it.item.id }) // today itself is not a memory
    }
    @Test fun nothingWhenNoMatch() = assertEquals(0, DiaryMemories.onThisDay(listOf(item(1, today.minusDays(5))), today).size)
    @Test fun leapDayLooksBackToFeb28InNormalYears() {
        val leap = LocalDate.of(2028, 2, 29)
        assertEquals(listOf(1), DiaryMemories.onThisDay(listOf(item(1, LocalDate.of(2027, 2, 28))), leap).map { it.yearsAgo })
    }
    @Test fun keepsTheNewestEntryOfThatDayAndCapsTheList() {
        val items = (1..6).map { item(it.toLong(), today.minusYears(it.toLong())) }
        assertEquals(3, DiaryMemories.onThisDay(items, today).size)
    }
}
