package com.spcode.tnpscprep.domain

import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

object DiaryLimits {
    const val MAX_CHARS = 20_000
    const val SEARCH_LIMIT = 200
}

/** A timeline row: only a short preview of the text is ever loaded for the list. */
data class DiaryItem(val id: Long, val date: LocalDate, val mood: String?, val preview: String, val pinned: Boolean = false, val wrapup: Boolean = false)

data class MonthGroup(val month: YearMonth, val items: List<DiaryItem>)

object DiaryTimeline {
    /** Groups an already newest-first list by calendar month, keeping the order. */
    fun groupByMonth(items: List<DiaryItem>): List<MonthGroup> {
        val out = ArrayList<MonthGroup>()
        for (it in items) {
            val m = YearMonth.from(it.date)
            if (out.isNotEmpty() && out.last().month == m) out[out.lastIndex] = out.last().copy(items = out.last().items + it)
            else out += MonthGroup(m, listOf(it))
        }
        return out
    }
}

/** What the editor should do after a change. Pure so the "debounce" rules are testable without a clock or a database. */
object Autosave {
    const val DEBOUNCE_MS = 800L
    enum class Action { NONE, SAVE, DELETE }

    /**
     * [msSinceEdit]: time since the last keystroke. [leaving]: she is closing the editor / the app is stopping (flush now).
     * - Nothing changed -> nothing to do.
     * - Too soon after the last keystroke -> wait (unless leaving).
     * - A blank NEW entry is never stored; a blank existing entry is deleted only when she LEAVES (not mid-typing).
     */
    fun decide(text: String, savedText: String?, isNew: Boolean, msSinceEdit: Long, leaving: Boolean): Action {
        if (text == (savedText ?: "")) return Action.NONE
        if (!leaving && msSinceEdit < DEBOUNCE_MS) return Action.NONE
        if (text.isBlank()) return if (isNew) Action.NONE else if (leaving) Action.DELETE else Action.NONE
        return Action.SAVE
    }
}

object DiarySearch {
    /** "%query%" for `LIKE ? ESCAPE '\'`, or null for a blank query. NFC-normalised (the same Tamil letter can be typed two ways); % _ \ are escaped. */
    fun likePattern(query: String): String? {
        val q = Normalizer.normalize(query.trim(), Normalizer.Form.NFC)
        if (q.isEmpty()) return null
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        return "%$escaped%"
    }
}

object DiaryPrompts {
    /** One prompt per day, rotating through the list; the same all day so it does not jump while she types. */
    fun indexFor(day: LocalDate, size: Int): Int = if (size <= 0) 0 else day.dayOfYear % size
}

/** Weekly review: counts only. No interpretation, no advice. */
data class DiaryWeek(val entries: Int, val moods: Map<String, Int>, val studyMinutes: Int)

object DiaryReview {
    fun week(items: List<DiaryItem>, today: LocalDate, studyMinutesByDate: Map<LocalDate, Int>): DiaryWeek {
        val days = FoodCheckin.weekOf(today).toSet()
        val mine = items.filter { it.date in days }
        val moods = mine.mapNotNull { it.mood }.groupingBy { it }.eachCount()
        return DiaryWeek(mine.size, moods, days.sumOf { studyMinutesByDate[it] ?: 0 })
    }
}

/** Writing streak and word count: pure, derived from the dates she wrote on / the text itself. */
object DiaryStats {
    /** Consecutive days with at least one entry, ending today. A day not written yet does not break it (the streak may end yesterday). */
    fun streak(dates: Set<LocalDate>, today: LocalDate): Int {
        var d = if (today in dates) today else today.minusDays(1)
        var n = 0
        while (d in dates) { n++; d = d.minusDays(1) }
        return n
    }

    /** Words separated by whitespace: works for Tamil too (it is space-separated). */
    fun words(text: String): Int = DiaryLinks.preview(DiaryPhotos.preview(text, " ")).trim().split(Regex("\\s+")).count { it.isNotEmpty() }
}
