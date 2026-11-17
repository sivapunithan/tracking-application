package com.spcode.tnpscprep.domain

import java.time.LocalDate

object StreakCalculator {
    /**
     * Consecutive days (ending today) on which total minutes >= daily goal.
     * If today's goal isn't met YET, the streak is counted up to yesterday: the day isn't over,
     * so it must not break the streak. Computed from the log each time (not stored), so
     * changing the goal re-evaluates history correctly.
     */
    fun currentStreak(minutesByDate: Map<LocalDate, Int>, dailyGoalMinutes: Int, today: LocalDate): Int {
        val goal = dailyGoalMinutes.coerceAtLeast(1)
        fun met(d: LocalDate) = (minutesByDate[d] ?: 0) >= goal
        var day = if (met(today)) today else today.minusDays(1)
        var streak = 0
        while (met(day)) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}
