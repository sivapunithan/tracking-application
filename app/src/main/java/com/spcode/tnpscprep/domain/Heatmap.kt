package com.spcode.tnpscprep.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The three fields of an activity_event the heatmap needs (keeps this file free of Room/Android). */
data class HeatEvent(val localDate: String, val type: String, val details: String)

/** One calendar day: how many focus minutes, how many events in all, and a 0..4 colour level. */
data class DaySummary(val date: LocalDate, val minutes: Int, val events: Int, val level: Int)

object Heatmap {
    const val MAX_LEVEL = 4
    private val MINUTES = Regex("\"minutes\"\\s*:\\s*(\\d+)")

    /** Focus minutes carried by one event (0 for anything but a focus session). */
    fun minutesOf(e: HeatEvent): Int =
        if (e.type != ActivityType.FOCUS_SESSION.name) 0 else MINUTES.find(e.details)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    /**
     * 0 = nothing happened. 1 = something happened but under a third of the goal (also revising topics
     * with no timer). 2 = a third, 3 = two thirds, 4 = goal reached (the darkest cell).
     */
    fun level(minutes: Int, events: Int, goal: Int): Int {
        if (events == 0) return 0
        if (goal <= 0) return if (minutes > 0) MAX_LEVEL else 1
        return when {
            minutes >= goal -> 4
            minutes * 3 >= goal * 2 -> 3
            minutes * 3 >= goal -> 2
            else -> 1
        }
    }

    fun summarise(events: List<HeatEvent>, goal: Int): Map<LocalDate, DaySummary> =
        events.groupBy { it.localDate }.mapNotNull { (d, list) ->
            val date = runCatching { LocalDate.parse(d) }.getOrNull() ?: return@mapNotNull null
            val minutes = list.sumOf(::minutesOf)
            date to DaySummary(date, minutes, list.size, level(minutes, list.size, goal))
        }.toMap()

    /**
     * Columns of weeks (Monday first, 7 cells each, oldest week first) ending with the week of [today].
     * Days after today are null so the grid never shows the future.
     */
    fun grid(summary: Map<LocalDate, DaySummary>, today: LocalDate, weeks: Int): List<List<DaySummary?>> {
        val lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val firstMonday = lastMonday.minusWeeks((weeks - 1).toLong())
        return (0 until weeks).map { w ->
            (0 until 7).map { d ->
                val date = firstMonday.plusWeeks(w.toLong()).plusDays(d.toLong())
                if (date.isAfter(today)) null else summary[date] ?: DaySummary(date, 0, 0, 0)
            }
        }
    }

    /** First date the grid shows: what the DAO has to load. */
    fun firstDate(today: LocalDate, weeks: Int): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((weeks - 1).toLong())
}
