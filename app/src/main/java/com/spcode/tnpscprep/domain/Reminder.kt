package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.ZoneId

/** "Wrapped up 5 of the last 7 evenings": a plain count, never a streak that resets. Days with no wrap-up are simply not counted. */
object WrapupConsistency {
    fun lastDays(wrapupDates: Set<LocalDate>, today: LocalDate, days: Int = 7): Int =
        (0 until days).count { today.minusDays(it.toLong()) in wrapupDates }
}

/** When the optional evening reminder should fire next. Pure so it is testable without Android. */
object ReminderTime {
    const val DEFAULT_MINUTES = 21 * 60
    const val STEP = 15

    fun clamp(minutes: Int): Int = ((minutes % 1440) + 1440) % 1440

    /** The next moment strictly after [nowMs] whose local time is [minutesOfDay] (today if still ahead, otherwise tomorrow). */
    fun nextTrigger(nowMs: Long, minutesOfDay: Int, zone: ZoneId): Long {
        val m = clamp(minutesOfDay)
        val now = java.time.Instant.ofEpochMilli(nowMs).atZone(zone)
        var at = now.toLocalDate().atTime(m / 60, m % 60).atZone(zone)
        if (!at.isAfter(now)) at = now.toLocalDate().plusDays(1).atTime(m / 60, m % 60).atZone(zone)
        return at.toInstant().toEpochMilli()
    }

    /** No notification when she has already wrapped up today, or the reminder is off. */
    fun shouldNotify(enabled: Boolean, wrappedToday: Boolean): Boolean = enabled && !wrappedToday
}
