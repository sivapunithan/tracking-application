package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthTest {
    @Test fun bmiAndBands() {
        assertEquals(22.86, Health.bmi(70.0, 175.0)!!, 0.01)
        assertNull(Health.bmi(0.0, 175.0))
        assertEquals(BmiBand.UNDER, Health.bmiBand(18.4))
        assertEquals(BmiBand.NORMAL, Health.bmiBand(18.5))
        assertEquals(BmiBand.OVER, Health.bmiBand(25.0))
        assertEquals(BmiBand.OBESE, Health.bmiBand(30.0))
    }

    @Test fun bmrMifflin() {
        assertEquals(1648.75, Health.bmr(70.0, 175.0, 30, Sex.MALE)!!, 0.01)
        assertEquals(1482.75, Health.bmr(70.0, 175.0, 30, Sex.FEMALE)!!, 0.01)
        assertNull(Health.bmr(70.0, 175.0, 5, Sex.MALE))
        assertEquals(2556, Health.tdee(1648.75, ActivityLevel.MODERATE))
    }

    @Test fun whrAndWater() {
        assertEquals(0.8, Health.waistHipRatio(80.0, 100.0)!!, 1e-9)
        assertTrue(Health.whrRaised(0.86, Sex.FEMALE))
        assertFalse(Health.whrRaised(0.86, Sex.MALE))
        assertEquals(2500, Health.waterMl(70.0))
        assertNull(Health.waterMl(5.0))
    }

    @Test fun cycleEstimate() {
        val e = Cycle.estimate(LocalDate.of(2026, 11, 1), 28)!!
        assertEquals(LocalDate.of(2026, 11, 29), e.nextPeriod)
        assertEquals(LocalDate.of(2026, 11, 15), e.ovulation)
        assertEquals(LocalDate.of(2026, 11, 10), e.fertileFrom)
        assertEquals(LocalDate.of(2026, 11, 16), e.fertileTo)
        assertNull(Cycle.estimate(LocalDate.of(2026, 11, 1), 10))
    }

    @Test fun cycleAverage() {
        val d = { m: Int, day: Int -> LocalDate.of(2026, m, day) }
        assertNull(Cycle.averageLength(listOf(d(1, 1))))
        assertEquals(29, Cycle.averageLength(listOf(d(1, 1), d(1, 30), d(2, 27))))
    }

    @Test fun breakNudge() {
        assertFalse(BreakNudge.due(49)); assertTrue(BreakNudge.due(50))
    }
}
