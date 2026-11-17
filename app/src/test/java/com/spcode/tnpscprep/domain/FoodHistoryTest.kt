package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodHistoryTest {
    private val today = LocalDate.of(2026, 11, 18) // Wednesday
    @Test fun countsWindowOnly() {
        val m = mapOf(today to FoodState.HEALTHY, today.minusDays(1) to FoodState.JUNK, today.minusDays(29) to FoodState.MIXED, today.minusDays(30) to FoodState.HEALTHY)
        val s = FoodHistory.summary(m, today, 30)
        assertEquals(FoodSummary(1, 1, 1, 27), s)
        assertEquals(3, s.logged)
    }
    @Test fun weeksAreMondayFirstAndNoFuture() {
        val w = FoodHistory.weeks(today, 2)
        assertEquals(2, w.size)
        assertEquals(LocalDate.of(2026, 11, 9), w[0][0])
        assertEquals(LocalDate.of(2026, 11, 16), w[1][0])
        assertEquals(today, w[1][2])
        assertNull(w[1][3])
    }
}
