package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CareStatsTest {
    private val today = LocalDate.of(2026, 11, 17)
    @Test fun targetFromWeightOrDefault() {
        assertEquals(2000, CareStats.waterTarget(null))
        assertEquals(2000, CareStats.waterTarget(500.0)) // not a plausible weight: default
        assertEquals(2100, CareStats.waterTarget(60.0))   // 60 kg * 35 ml = 2100
    }
    @Test fun waterDaysSumPerDayOldestFirstWithZeros() {
        val e = listOf(today to 250, today to 250, today.minusDays(2) to 500, today.minusDays(30) to 999)
        val w = CareStats.waterDays(e, today, 3)
        assertEquals(listOf(today.minusDays(2) to 500, today.minusDays(1) to 0, today to 500), w)
    }
    @Test fun fractionIsClamped() {
        assertEquals(0.5f, CareStats.fraction(1000, 2000), 0.0001f)
        assertEquals(1f, CareStats.fraction(5000, 2000), 0.0001f)
        assertEquals(0f, CareStats.fraction(100, 0), 0.0001f)
    }
}
