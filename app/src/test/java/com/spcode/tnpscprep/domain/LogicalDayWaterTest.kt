package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression: a glass of water at 00:30 must be saved under the SAME day that Care and Today read (the 3 AM logical day). */
class LogicalDayWaterTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private fun ms(h: Int, m: Int, day: Int) = ZonedDateTime.of(2026, 11, day, h, m, 0, 0, z).toInstant().toEpochMilli()

    @Test fun halfPastMidnightBelongsToThePreviousDay() {
        assertEquals(LocalDate.of(2026, 11, 17), LogicalDay.of(ms(0, 30, 18), z))
        assertEquals(LocalDate.of(2026, 11, 17), LogicalDay.of(ms(2, 59, 18), z))
        assertEquals(LocalDate.of(2026, 11, 18), LogicalDay.of(ms(3, 0, 18), z))
    }

    @Test fun waterLoggedAtNightShowsInTodaysRing() {
        val day = LogicalDay.of(ms(0, 30, 18), z)
        val week = CareStats.waterDays(listOf(day to 250, day to 500), day)
        assertEquals(750, week.last().second)
    }
}
