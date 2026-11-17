package com.spcode.tnpscprep.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Simple spaced-revision rule: the higher the level, the longer until the topic should be seen again.
 * Level 0 (not started) is never "due" (it is simply unread). Intervals are defaults; they live in
 * one place so they are easy to change.
 */
object RevisionSchedule {
    private val INTERVAL_DAYS = mapOf(
        RevisionLevel.FIRST_READ to 3,
        RevisionLevel.REVISION_1 to 7,
        RevisionLevel.REVISION_2 to 21,
        RevisionLevel.REVISION_3 to 60, // finished the cycle: a maintenance review
    )

    fun intervalDays(level: Int): Int? = INTERVAL_DAYS[RevisionLevel.clamp(level)]

    /** Local calendar date the topic is due, or null if it has no schedule (level 0). */
    fun dueDate(level: Int, lastUpdatedMs: Long, zone: ZoneId): LocalDate? {
        val days = intervalDays(level) ?: return null
        return Instant.ofEpochMilli(lastUpdatedMs).atZone(zone).toLocalDate().plusDays(days.toLong())
    }

    /** 0 = due today, positive = overdue by that many days, negative = not due yet. */
    fun daysOverdue(due: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(due, today)
}
