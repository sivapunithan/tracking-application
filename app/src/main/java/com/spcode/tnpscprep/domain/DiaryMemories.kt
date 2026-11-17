package com.spcode.tnpscprep.domain

import java.time.LocalDate

data class Memory(val yearsAgo: Int, val item: DiaryItem)

/** "On this day": diary entries written on today's date in earlier years (1..[maxYears] years back), nearest year first. Pure. */
object DiaryMemories {
    fun onThisDay(items: List<DiaryItem>, today: LocalDate, maxYears: Int = 10, limit: Int = 3): List<Memory> {
        val out = ArrayList<Memory>()
        for (n in 1..maxYears) {
            val d = today.minusYears(n.toLong()) // 29 Feb -> 28 Feb in a non-leap year
            items.firstOrNull { it.date == d }?.let { out += Memory(n, it) } // items are newest-first, so this is that day's latest entry
            if (out.size >= limit) break
        }
        return out
    }
}
