package com.spcode.tnpscprep.domain

/** Which mood picture shows for today's plan progress. Pure. Never a sad face in the morning; WORRIED only in the evening with nothing ticked. */
object PlanMood {
    const val MAX_LINES = 6
    const val MAX_TITLE = 80
    const val WORRY_HOUR = 18

    /** Returns a Mood name. An empty plan = CRY (nothing planned yet). */
    fun of(done: Int, total: Int, hour: Int): String = when {
        total <= 0 -> "CRY"
        done >= total -> "CELEBRATE"
        done * 2 >= total -> "LAUGH"
        done > 0 -> "CHEER"
        hour >= WORRY_HOUR -> "WORRIED"
        else -> "PROUD"
    }

    /** A line she typed: trimmed, capped; null if blank. */
    fun cleanTitle(s: String): String? = s.trim().take(MAX_TITLE).ifBlank { null }
}
