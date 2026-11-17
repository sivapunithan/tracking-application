package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyCycleTest {
    private val d = { m: Int, day: Int -> LocalDate.of(2026, m, day) }

    @Test fun noStartsMeansNoStatus() = assertNull(BodyCycle.cycleStatus(emptyList(), 28, d(11, 17)))

    @Test fun oneStartUsesTheSetLengthAndCountsTheDay() {
        val s = BodyCycle.cycleStatus(listOf(d(11, 10)), 28, d(11, 17))!!
        assertEquals(8, s.dayOfCycle); assertEquals(28, s.cycleLength); assertEquals(0, s.cyclesUsed)
        assertEquals(d(12, 8), s.estimate.nextPeriod); assertEquals(21, s.daysToNext)
    }

    @Test fun averageOfLoggedGapsWinsOverTheSetLength() {
        val s = BodyCycle.cycleStatus(listOf(d(9, 1), d(9, 30), d(10, 29)), 35, d(11, 1))!!   // gaps 29, 29
        assertEquals(29, s.cycleLength); assertEquals(2, s.cyclesUsed)
    }

    @Test fun phasesFollowTheDay() {
        val start = d(11, 1)
        assertEquals(CyclePhase.PERIOD, BodyCycle.cycleStatus(listOf(start), 28, d(11, 3))!!.phase)
        // 28-day cycle: next 11/29, ovulation 11/15, fertile 11/10..11/16
        assertEquals(CyclePhase.FERTILE, BodyCycle.cycleStatus(listOf(start), 28, d(11, 12))!!.phase)
        assertEquals(CyclePhase.LATER, BodyCycle.cycleStatus(listOf(start), 28, d(11, 22))!!.phase)
    }

    @Test fun pastTheEstimateIsNegativeNotAnError() {
        val s = BodyCycle.cycleStatus(listOf(d(10, 1)), 28, d(11, 3))!!    // next was 10/29
        assertEquals(-5, s.daysToNext)
    }

    @Test fun futureStartsAreIgnored() = assertNull(BodyCycle.cycleStatus(listOf(d(12, 1)), 28, d(11, 17)))

    @Test fun weightChangeNeedsTwoReadingsInTheWindow() {
        val today = d(11, 17)
        assertNull(BodyCycle.weightChange(listOf(d(11, 10) to 60.0), today))
        assertEquals(-1.5, BodyCycle.weightChange(listOf(d(10, 25) to 61.5, d(11, 16) to 60.0, d(8, 1) to 70.0), today)!!, 0.001)
    }
}
