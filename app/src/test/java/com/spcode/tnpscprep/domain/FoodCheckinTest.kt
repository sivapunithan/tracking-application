package com.spcode.tnpscprep.domain

import com.spcode.tnpscprep.domain.FoodState.HEALTHY
import com.spcode.tnpscprep.domain.FoodState.JUNK
import com.spcode.tnpscprep.domain.FoodState.MIXED
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCheckinTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private val tue = LocalDate.of(2026, 11, 17) // a Tuesday
    private fun ms(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(z).toInstant().toEpochMilli()
    private fun days(vararg p: Pair<Int, FoodState>) = p.associate { tue.minusDays(it.first.toLong()) to it.second }

    @Test fun rolloverAtThreeAm() {
        assertEquals(tue.minusDays(1), FoodCheckin.foodDay(ms(tue, 0, 30), z)) // 12:30 AM is still yesterday
        assertEquals(tue.minusDays(1), FoodCheckin.foodDay(ms(tue, 2, 59), z))
        assertEquals(tue, FoodCheckin.foodDay(ms(tue, 3, 0), z))
        assertEquals(tue, FoodCheckin.foodDay(ms(tue, 23, 59), z))
    }

    @Test fun weekIsMondayToSunday() {
        val w = FoodCheckin.weekOf(tue)
        assertEquals(LocalDate.of(2026, 11, 16), w.first()); assertEquals(LocalDate.of(2026, 11, 22), w.last())
    }

    @Test fun weeklyCountOnlyThisWeekAndOnlyHealthy() {
        val s = days(0 to HEALTHY, 1 to HEALTHY, 2 to HEALTHY /* Sunday: last week */, 3 to MIXED)
        assertEquals(2, FoodCheckin.healthyThisWeek(s, tue))
    }

    @Test fun streakCountsHealthyAndTodayOpenDoesNotBreak() {
        assertEquals(3, FoodCheckin.streak(days(1 to HEALTHY, 2 to HEALTHY, 3 to HEALTHY), tue)) // today unlogged
        assertEquals(4, FoodCheckin.streak(days(0 to HEALTHY, 1 to HEALTHY, 2 to HEALTHY, 3 to HEALTHY), tue))
        assertEquals(0, FoodCheckin.streak(emptyMap(), tue))
    }

    @Test fun oneGraceDayPerWeekThenBreak() {
        // Mon(1 day ago) healthy, Sun missing is last week's grace, Sat healthy ...
        val oneMiss = days(0 to HEALTHY, 1 to HEALTHY, 3 to HEALTHY, 4 to HEALTHY) // 2 days ago (Sunday) missing = grace of last week
        assertEquals(4, FoodCheckin.streak(oneMiss, tue))
        // two misses in the SAME week (Sat + Sun) -> the second one breaks
        val twoMisses = days(0 to HEALTHY, 1 to HEALTHY, 4 to HEALTHY, 5 to HEALTHY) // 2 and 3 days ago = Sun, Sat
        assertEquals(2, FoodCheckin.streak(twoMisses, tue))
    }

    @Test fun mixedAndJunkUseTheGraceDayToo() {
        assertEquals(3, FoodCheckin.streak(days(0 to HEALTHY, 1 to MIXED, 2 to HEALTHY, 3 to HEALTHY), tue))
    }

    @Test fun chainOfThreeJunk() {
        val c = FoodCheckin.junkChain(days(0 to JUNK, 1 to JUNK, 2 to JUNK), tue)
        assertEquals(3, c.length); assertEquals(tue.minusDays(2), c.start)
    }

    @Test fun mixedOrHealthyBreaksChain() {
        assertEquals(1, FoodCheckin.junkChain(days(0 to JUNK, 1 to MIXED, 2 to JUNK, 3 to JUNK), tue).length)
        assertEquals(0, FoodCheckin.junkChain(days(0 to HEALTHY, 1 to JUNK, 2 to JUNK, 3 to JUNK), tue).length)
    }

    @Test fun unloggedDayDoesNotCountAndBreaksAdjacency() {
        assertEquals(1, FoodCheckin.junkChain(days(0 to JUNK, 2 to JUNK, 3 to JUNK), tue).length)
        // today still unlogged: yesterday's chain is alive, not broken
        assertEquals(2, FoodCheckin.junkChain(days(1 to JUNK, 2 to JUNK), tue).length)
        assertEquals(0, FoodCheckin.junkChain(emptyMap(), tue).length)
    }

    @Test fun alertFiresOncePerChainAndOnlyInStrictMode() {
        val chain = FoodCheckin.junkChain(days(0 to JUNK, 1 to JUNK, 2 to JUNK), tue)
        assertTrue(FoodCheckin.shouldAlert(chain, strict = true, lastAlertedStart = null))
        assertFalse(FoodCheckin.shouldAlert(chain, strict = false, lastAlertedStart = null))
        assertFalse(FoodCheckin.shouldAlert(chain, strict = true, lastAlertedStart = chain.start)) // already shown
        // the same chain growing to 4 days keeps its start: still no repeat
        // next day she logs JUNK again: the chain is 4 long but has the SAME start, so no repeat
        val nextDay = tue.plusDays(1)
        val longer = FoodCheckin.junkChain(days(0 to JUNK, 1 to JUNK, 2 to JUNK) + (nextDay to JUNK), nextDay)
        assertEquals(4, longer.length); assertEquals(chain.start, longer.start)
        assertFalse(FoodCheckin.shouldAlert(longer, strict = true, lastAlertedStart = chain.start))
    }

    @Test fun resetAfterNonJunkAllowsNewAlert() {
        val first = FoodCheckin.junkChain(days(5 to JUNK, 6 to JUNK, 7 to JUNK), tue.minusDays(5))
        // later: a healthy day broke it, then a brand-new chain of 3 starts on a different date
        val next = FoodCheckin.junkChain(days(0 to JUNK, 1 to JUNK, 2 to JUNK, 3 to HEALTHY), tue)
        assertTrue(FoodCheckin.shouldAlert(next, strict = true, lastAlertedStart = first.start))
    }

    @Test fun shortChainNeverAlerts() {
        assertFalse(FoodCheckin.shouldAlert(FoodCheckin.junkChain(days(0 to JUNK, 1 to JUNK), tue), true, null))
        assertNull(FoodCheckin.junkChain(days(0 to MIXED), tue).start)
    }

    @Test fun eveningPromptOnlyAfterSixWhenUnlogged() {
        assertTrue(FoodCheckin.eveningPrompt(ms(tue, 18), z, false))
        assertFalse(FoodCheckin.eveningPrompt(ms(tue, 17, 59), z, false))
        assertFalse(FoodCheckin.eveningPrompt(ms(tue, 20), z, true))
    }
}
