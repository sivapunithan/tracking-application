package com.spcode.tnpscprep.domain

/** Tiny pure helpers for the home-screen widgets (unit-tested). Widgets show study facts only: never diary, health or mood. */
object WidgetRules {
    const val MAX_PLAN_LINES = 3

    /** Plan lines for the widget: unfinished first, at most [MAX_PLAN_LINES], text trimmed so a long headline cannot overflow. */
    fun planLines(tasks: List<Pair<String, Boolean>>): List<Pair<String, Boolean>> =
        tasks.sortedBy { it.second }.take(MAX_PLAN_LINES).map { it.first.trim().take(70) to it.second }

    fun minutesLine(minutes: Int, goal: Int): Pair<Int, Int> = minutes.coerceAtLeast(0) to goal.coerceAtLeast(1)

    /** Exam countdown for the widget: null when no date is set or the date has passed. */
    fun countdown(daysToExam: Long?): Long? = daysToExam?.takeIf { it >= 0 }
}
