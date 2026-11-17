package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CyclePhase { PERIOD, FERTILE, LATER }

/**
 * Where she is in her cycle today, derived from logged period STARTS only (pure, tested). All values are estimates:
 * [daysToNext] is negative when the estimate has passed (cycles vary, so that is shown neutrally, never as a problem).
 */
data class CycleStatus(
    val lastStart: LocalDate, val cycleLength: Int, val cyclesUsed: Int, val dayOfCycle: Int,
    val estimate: CycleEstimate, val daysToNext: Int, val phase: CyclePhase,
)

object BodyCycle {
    /** Starts in the future are ignored. [defaultLen] is her set length, used until two periods are logged. Null when nothing usable is logged. */
    fun cycleStatus(starts: List<LocalDate>, defaultLen: Int, today: LocalDate): CycleStatus? {
        val past = starts.filter { !it.isAfter(today) }.distinct().sorted()
        val last = past.lastOrNull() ?: return null
        val avg = Cycle.averageLength(past)
        val len = (avg ?: defaultLen).coerceIn(Cycle.MIN_DAYS, Cycle.MAX_DAYS)
        val est = Cycle.estimate(last, len) ?: return null
        val day = ChronoUnit.DAYS.between(last, today).toInt() + 1
        val phase = when {
            day <= 5 -> CyclePhase.PERIOD
            !today.isBefore(est.fertileFrom) && !today.isAfter(est.fertileTo) -> CyclePhase.FERTILE
            else -> CyclePhase.LATER
        }
        return CycleStatus(last, len, if (avg != null) past.size - 1 else 0, day, est, ChronoUnit.DAYS.between(today, est.nextPeriod).toInt(), phase)
    }

    /** Weight change over the last [days] days: latest minus the earliest reading inside the window (null with fewer than two readings). */
    fun weightChange(readings: List<Pair<LocalDate, Double>>, today: LocalDate, days: Int = 30): Double? {
        val win = readings.filter { !it.first.isAfter(today) && !it.first.isBefore(today.minusDays(days.toLong())) }.sortedBy { it.first }
        if (win.size < 2) return null
        return win.last().second - win.first().second
    }
}
