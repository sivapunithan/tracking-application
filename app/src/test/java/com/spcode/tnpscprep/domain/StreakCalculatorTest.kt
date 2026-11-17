package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 11, 17)
    private fun d(back: Long) = today.minusDays(back)

    @Test fun no_sessions_is_zero() =
        assertEquals(0, StreakCalculator.currentStreak(emptyMap(), 60, today))

    @Test fun only_today_meeting_goal_is_one() =
        assertEquals(1, StreakCalculator.currentStreak(mapOf(d(0) to 60), 60, today))

    @Test fun consecutive_days_including_today() {
        val m = mapOf(d(0) to 70, d(1) to 60, d(2) to 90)
        assertEquals(3, StreakCalculator.currentStreak(m, 60, today))
    }

    @Test fun today_not_yet_met_does_not_break_streak() {
        val m = mapOf(d(0) to 10, d(1) to 60, d(2) to 60)
        assertEquals(2, StreakCalculator.currentStreak(m, 60, today))
    }

    @Test fun missed_yesterday_and_today_resets_to_zero() {
        val m = mapOf(d(2) to 120, d(3) to 120)
        assertEquals(0, StreakCalculator.currentStreak(m, 60, today))
    }

    @Test fun a_gap_ends_the_streak() {
        val m = mapOf(d(0) to 60, d(1) to 60, d(2) to 30, d(3) to 60)
        assertEquals(2, StreakCalculator.currentStreak(m, 60, today))
    }

    @Test fun below_goal_day_does_not_count() =
        assertEquals(0, StreakCalculator.currentStreak(mapOf(d(1) to 59), 60, today))

    @Test fun changing_goal_re_evaluates_history() {
        val m = mapOf(d(0) to 30, d(1) to 30, d(2) to 30)
        assertEquals(3, StreakCalculator.currentStreak(m, 30, today))
        assertEquals(0, StreakCalculator.currentStreak(m, 45, today))
    }

    @Test fun nonsense_goal_is_treated_as_one_minute() {
        val m = mapOf(d(0) to 1)
        assertEquals(1, StreakCalculator.currentStreak(m, 0, today))
        assertEquals(1, StreakCalculator.currentStreak(m, -10, today))
    }
}
