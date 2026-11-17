package com.spcode.tnpscprep.domain

/** 0 Not started, 1 First read, 2 Revision 1, 3 Revision 2, 4 Revision 3. */
object RevisionLevel {
    const val NOT_STARTED = 0
    const val FIRST_READ = 1
    const val REVISION_1 = 2
    const val REVISION_2 = 3
    const val REVISION_3 = 4
    const val MAX = REVISION_3

    fun clamp(level: Int): Int = level.coerceIn(NOT_STARTED, MAX)

    /** Tap: go up one level. Stops at MAX (it does NOT wrap to 0, so a stray tap can't erase progress). */
    fun advance(level: Int): Int = (clamp(level) + 1).coerceAtMost(MAX)

    /** Long-press. */
    fun reset(): Int = NOT_STARTED

    /** Completion 0..100 for a set of topic levels: each level counts MAX steps' worth, so partial revision shows. */
    fun percent(levels: List<Int>): Int {
        if (levels.isEmpty()) return 0
        val total = levels.sumOf { clamp(it) }
        val denom = MAX * levels.size
        return (total * 100 + denom / 2) / denom
    }
}
