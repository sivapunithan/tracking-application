package com.spcode.tnpscprep.ui.timer

import com.spcode.tnpscprep.data.db.DayTotal
import com.spcode.tnpscprep.domain.StreakCalculator
import java.time.LocalDate

data class DayStats(val todayMinutes: Int, val streak: Int, val daysSinceStudy: Int? = null)

/** Pure: turns the per-day totals from Room into "today's minutes" and the current streak. */
fun buildDayStats(totals: List<DayTotal>, goalMinutes: Int, today: LocalDate): DayStats {
    val byDate = totals.associate { LocalDate.parse(it.date) to it.minutes }
    val last = byDate.filterValues { it > 0 }.keys.maxOrNull()
    val since = last?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) }
    return DayStats(byDate[today] ?: 0, StreakCalculator.currentStreak(byDate, goalMinutes, today), since)
}
