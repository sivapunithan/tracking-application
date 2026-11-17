package com.spcode.tnpscprep.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The app's "day" changes at 3 AM local time, so something done at 12:30 AM belongs to the previous day. Shared by the food check-in and the diary. */
object LogicalDay {
    const val ROLLOVER_HOUR = 3L

    fun of(nowMs: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(nowMs).atZone(zone).minusHours(ROLLOVER_HOUR).toLocalDate()
}
