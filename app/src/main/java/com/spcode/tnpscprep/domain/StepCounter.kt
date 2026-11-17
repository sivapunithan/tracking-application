package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * One row of step state. [baseline] = sensor reading at the start of [date]'s count, [lastReading] = the reading at the last app open,
 * [steps] = the day's total so far, [bootMs] = estimated boot time (now - elapsedRealtime) at that read.
 */
data class StepDay(val date: LocalDate, val baseline: Long, val lastReading: Long, val steps: Int, val bootMs: Long)

/**
 * TYPE_STEP_COUNTER reports a running total since boot and we only look at it when the app opens (no background job),
 * so every read adds "reading - lastReading" to the current day. Steps taken since the previous open are credited to the day of THIS read (approximate).
 */
object StepCounter {
    /** Boot time is derived from two clocks and drifts a little; more than this = a real reboot. */
    const val BOOT_TOLERANCE_MS = 120_000L

    fun update(prev: StepDay?, reading: Long, bootMs: Long, today: LocalDate): StepDay {
        val r = reading.coerceAtLeast(0)
        if (prev == null) return StepDay(today, r, r, 0, bootMs)

        val rebooted = r < prev.lastReading || abs(bootMs - prev.bootMs) > BOOT_TOLERANCE_MS
        // After a reboot the counter restarted at 0 sometime after the last read, so everything in [r] is new.
        val delta = (if (rebooted) r else r - prev.lastReading).coerceAtLeast(0)

        val gap = ChronoUnit.DAYS.between(prev.date, today)
        return when {
            gap <= 0L -> prev.copy(lastReading = r, steps = prev.steps + delta.toInt(), bootMs = bootMs)
            // First open on the next day: the steps since the last open are credited to today.
            gap == 1L -> StepDay(today, if (rebooted) 0 else prev.lastReading, r, delta.toInt(), bootMs)
            // Several days unopened: those steps cannot be placed on a day, so start fresh instead of guessing.
            else -> StepDay(today, r, r, 0, bootMs)
        }
    }
}
