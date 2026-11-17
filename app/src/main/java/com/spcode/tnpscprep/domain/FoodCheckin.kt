package com.spcode.tnpscprep.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class FoodState { HEALTHY, MIXED, JUNK }

/** Everything about the food check-in that is a RULE. Pure and derived from the day -> state map, so rules can change with no migration. */
object FoodCheckin {
    const val ROLLOVER_HOUR = 3L
    const val CHAIN_LENGTH = 3
    private const val MAX_LOOKBACK_DAYS = 400

    /** The "food day" a moment belongs to: the day changes at 3 AM, so 12:30 AM still counts for the previous day. */
    fun foodDay(nowMs: Long, zone: ZoneId): LocalDate = LogicalDay.of(nowMs, zone)

    fun weekOf(day: LocalDate): List<LocalDate> {
        val monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun healthyThisWeek(states: Map<LocalDate, FoodState>, today: LocalDate): Int =
        weekOf(today).count { states[it] == FoodState.HEALTHY }

    /**
     * Consecutive HEALTHY days ending today. Today not being logged yet never breaks it. One day per Monday-Sunday week may be
     * non-healthy or unlogged (a grace day): it neither breaks the streak nor counts towards it. A second one in the same week breaks it.
     */
    fun streak(states: Map<LocalDate, FoodState>, today: LocalDate): Int {
        val earliest = states.keys.minOrNull() ?: return 0
        var count = 0
        val graceUsedInWeek = HashSet<LocalDate>() // keyed by the Monday of the week
        var d = today
        var steps = 0
        while (!d.isBefore(earliest) && steps++ < MAX_LOOKBACK_DAYS) {
            val s = states[d]
            when {
                s == FoodState.HEALTHY -> count++
                d == today && s == null -> Unit // still open
                else -> {
                    val monday = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    if (!graceUsedInWeek.add(monday)) return count
                }
            }
            d = d.minusDays(1)
        }
        return count
    }

    /** A run of consecutive calendar days logged JUNK, ending at today (or yesterday while today is unlogged). */
    data class Chain(val length: Int, val start: LocalDate?)

    fun junkChain(states: Map<LocalDate, FoodState>, today: LocalDate): Chain {
        var end = today
        if (states[end] == null) end = end.minusDays(1) // today still open: the chain is not broken yet
        if (states[end] != FoodState.JUNK) return Chain(0, null)
        var d = end
        var n = 0
        while (states[d] == FoodState.JUNK) { n++; d = d.minusDays(1) }
        return Chain(n, d.plusDays(1))
    }

    /** Strict mode only: show the nudge when a chain reaches 3 and this chain (identified by its start date) has not been shown. */
    fun shouldAlert(chain: Chain, strict: Boolean, lastAlertedStart: LocalDate?): Boolean =
        strict && chain.length >= CHAIN_LENGTH && chain.start != null && chain.start != lastAlertedStart

    /** The card gets a stronger look after 6 PM when today has no check-in. */
    fun eveningPrompt(nowMs: Long, zone: ZoneId, loggedToday: Boolean): Boolean {
        val hour = Instant.ofEpochMilli(nowMs).atZone(zone).hour
        return !loggedToday && hour >= 18
    }
}
