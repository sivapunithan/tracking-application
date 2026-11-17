package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RevisionScheduleTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val day0 = LocalDate.of(2026, 11, 17)
    private fun ms(d: LocalDate, hour: Int = 10) = d.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun intervals_grow_with_level() {
        assertEquals(listOf(3, 7, 21, 60), (1..4).map { RevisionSchedule.intervalDays(it) })
    }

    @Test fun not_started_has_no_schedule() {
        assertNull(RevisionSchedule.intervalDays(0))
        assertNull(RevisionSchedule.dueDate(0, ms(day0), zone))
    }

    @Test fun out_of_range_levels_are_clamped() {
        assertEquals(60, RevisionSchedule.intervalDays(99))
        assertNull(RevisionSchedule.intervalDays(-3))
    }

    @Test fun due_date_is_last_update_plus_interval() {
        assertEquals(day0.plusDays(3), RevisionSchedule.dueDate(1, ms(day0), zone))
        assertEquals(day0.plusDays(21), RevisionSchedule.dueDate(3, ms(day0), zone))
    }

    @Test fun the_calendar_day_not_the_hour_decides() { // 00:30 and 23:30 on the same day are due on the same date
        assertEquals(RevisionSchedule.dueDate(2, ms(day0, 0), zone), RevisionSchedule.dueDate(2, ms(day0, 23), zone))
    }

    @Test fun days_overdue_counts_from_the_due_date() {
        val due = day0
        assertEquals(0, RevisionSchedule.daysOverdue(due, day0))
        assertEquals(2, RevisionSchedule.daysOverdue(due, day0.plusDays(2)))
        assertEquals(-1, RevisionSchedule.daysOverdue(due, day0.minusDays(1)))
    }
}
