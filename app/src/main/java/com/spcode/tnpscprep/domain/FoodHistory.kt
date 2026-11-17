package com.spcode.tnpscprep.domain

import java.time.LocalDate

data class FoodSummary(val healthy: Int, val mixed: Int, val junk: Int, val unlogged: Int) {
    val logged get() = healthy + mixed + junk
}

/** Counts for the last [days] days ending [today] (inclusive). Derived from the day -> state map, nothing stored. Days without a check-in are "unlogged", never "bad". */
object FoodHistory {
    fun summary(map: Map<LocalDate, FoodState>, today: LocalDate, days: Int = 30): FoodSummary {
        var h = 0; var m = 0; var j = 0; var u = 0
        for (i in 0 until days) when (map[today.minusDays(i.toLong())]) {
            FoodState.HEALTHY -> h++
            FoodState.MIXED -> m++
            FoodState.JUNK -> j++
            null -> u++
        }
        return FoodSummary(h, m, j, u)
    }

    /** [weeks] Monday-first weeks ending with the week of [today]; days after today are null. */
    fun weeks(today: LocalDate, weeks: Int): List<List<LocalDate?>> {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return (weeks - 1 downTo 0).map { w ->
            val start = monday.minusWeeks(w.toLong())
            (0..6).map { d -> start.plusDays(d.toLong()).takeIf { !it.isAfter(today) } }
        }
    }
}
