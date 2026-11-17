package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.ZoneId

/** One day's phone use: the total over every app with at least a minute, plus the top apps (package -> minutes). */
data class DayUsage(val totalMinutes: Int, val topApps: List<Pair<String, Int>>)

object PhoneUsage {
    const val KEEP_DAYS = 90
    const val READ_DAYS = 7
    const val TOP_N = 10
    private const val MIN_MS = 60_000L

    /** Drops sub-minute apps, sums the rest and keeps the TOP_N (ties broken by package name so the result is stable). */
    fun summarise(foregroundMsByApp: Map<String, Long>, topN: Int = TOP_N): DayUsage {
        val apps = foregroundMsByApp.filterValues { it >= MIN_MS }.map { (p, ms) -> p to (ms / MIN_MS).toInt() }
        return DayUsage(apps.sumOf { it.second }, apps.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }).take(topN))
    }

    /** [start, end) of a calendar day in epoch millis; a day still in progress ends at [nowMs], a future day is empty. */
    fun window(date: LocalDate, nowMs: Long, zone: ZoneId): Pair<Long, Long> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to minOf(end, maxOf(nowMs, start))
    }

    /** Dates to refresh on one app open: today and the 6 days before it, oldest first. */
    fun datesToRead(today: LocalDate): List<LocalDate> = (READ_DAYS - 1 downTo 0).map { today.minusDays(it.toLong()) }

    fun label(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"

    /** Last segment of a package name, used only when Android hides the app's real name. */
    fun fallbackName(pkg: String): String = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}
