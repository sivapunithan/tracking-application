package com.spcode.tnpscprep.ui.timer

import com.spcode.tnpscprep.data.db.DayTotal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStatsTest {
    private val today = LocalDate.of(2026, 11, 17)
    private fun day(back: Long, min: Int) = DayTotal(today.minusDays(back).toString(), min)

    @Test fun nothing_logged() = assertEquals(DayStats(0, 0), buildDayStats(emptyList(), 60, today))

    @Test fun today_minutes_come_from_todays_total_only() {
        val s = buildDayStats(listOf(day(0, 40), day(1, 90)), 60, today)
        assertEquals(40, s.todayMinutes)
        assertEquals(1, s.streak) // today not met yet, yesterday met
    }

    @Test fun streak_uses_the_goal() {
        val totals = listOf(day(0, 30), day(1, 30), day(2, 30))
        assertEquals(3, buildDayStats(totals, 30, today).streak)
        assertEquals(0, buildDayStats(totals, 31, today).streak)
    }
}
