package com.spcode.tnpscprep.domain

object TimerLimits {
    val FOCUS = 5..90
    val BREAK = 1..30
    val GOAL = 15..600
    const val FOCUS_STEP = 5
    const val BREAK_STEP = 1
    const val GOAL_STEP = 15

    fun step(current: Int, delta: Int, range: IntRange): Int = (current + delta).coerceIn(range)
}
