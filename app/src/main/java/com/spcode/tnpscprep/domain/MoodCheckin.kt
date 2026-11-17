package com.spcode.tnpscprep.domain

import java.time.LocalDate

/** One logged row of a MOOD_CHECKIN event, as the parser needs it. */
data class MoodRow(val localDate: String, val details: String)

/** Everything about the daily mood check-in that is a RULE. Pure; the day -> mood map is derived from activity_event rows. */
object MoodCheckins {
    private val VALUE = Regex("\"value\"\\s*:\\s*\"(\\w+)\"")

    /** Rows are ordered by time, so the LATEST row of a day wins. Unknown or damaged rows are skipped. */
    fun parse(rows: List<MoodRow>): Map<LocalDate, Mood> = buildMap {
        rows.forEach { r ->
            val d = runCatching { LocalDate.parse(r.localDate) }.getOrNull() ?: return@forEach
            val m = VALUE.find(r.details)?.groupValues?.get(1)?.let { n -> Mood.entries.firstOrNull { it.name == n } } ?: return@forEach
            put(d, m)
        }
    }

    /** The [days] days ending [today], oldest first (for the 7-day mood strip). */
    fun lastDays(today: LocalDate, days: Int = 7): List<LocalDate> = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }

    /**
     * How many days each mood was logged over the last [days] days: counts only, most frequent first (ties keep the Mood order).
     * No interpretation, no score.
     */
    fun counts(map: Map<LocalDate, Mood>, today: LocalDate, days: Int = 30): List<Pair<Mood, Int>> {
        val window = lastDays(today, days).toSet()
        val c = map.filterKeys { it in window }.values.groupingBy { it }.eachCount()
        return Mood.entries.filter { (c[it] ?: 0) > 0 }.map { it to c.getValue(it) }.sortedByDescending { it.second }
    }
}
