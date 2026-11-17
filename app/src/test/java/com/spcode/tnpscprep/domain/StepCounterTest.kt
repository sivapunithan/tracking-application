package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StepCounterTest {
    private val d1 = LocalDate.of(2026, 11, 17)
    private val boot = 1_000_000L

    @Test fun firstReadStartsAtZero() {
        val s = StepCounter.update(null, 5000, boot, d1)
        assertEquals(0, s.steps); assertEquals(5000, s.baseline); assertEquals(5000, s.lastReading)
    }

    @Test fun sameDayAddsDelta() {
        val a = StepCounter.update(null, 5000, boot, d1)
        val b = StepCounter.update(a, 5800, boot + 500, d1)
        val c = StepCounter.update(b, 6000, boot + 900, d1)
        assertEquals(800, b.steps); assertEquals(1000, c.steps); assertEquals(5000, c.baseline)
    }

    @Test fun sameReadingChangesNothing() {
        val a = StepCounter.update(null, 5000, boot, d1)
        assertEquals(a, StepCounter.update(a, 5000, boot, d1))
    }

    @Test fun rebootByLowerReadingNeverGoesNegative() {
        val a = StepCounter.update(StepCounter.update(null, 5000, boot, d1), 5800, boot, d1) // 800 so far
        val b = StepCounter.update(a, 300, boot + 9_000_000, d1) // phone rebooted, counter restarted
        assertEquals(1100, b.steps); assertEquals(300, b.lastReading)
    }

    @Test fun rebootDetectedByBootTimeEvenWhenReadingIsHigher() {
        val a = StepCounter.update(StepCounter.update(null, 100, boot, d1), 200, boot, d1) // 100 so far
        val b = StepCounter.update(a, 900, boot + 3_600_000, d1) // rebooted, already walked past the old reading
        assertEquals(1000, b.steps) // 100 + all 900 new
    }

    @Test fun smallBootTimeDriftIsNotAReboot() {
        val a = StepCounter.update(null, 100, boot, d1)
        assertEquals(50, StepCounter.update(a, 150, boot + 5_000, d1).steps)
    }

    @Test fun nextDayCreditsStepsSinceLastOpen() {
        val a = StepCounter.update(StepCounter.update(null, 5000, boot, d1), 6000, boot, d1) // 1000 on d1
        val b = StepCounter.update(a, 6700, boot, d1.plusDays(1))
        assertEquals(d1.plusDays(1), b.date); assertEquals(700, b.steps); assertEquals(6000, b.baseline)
    }

    @Test fun nextDayAfterRebootStartsFromZeroBaseline() {
        val a = StepCounter.update(StepCounter.update(null, 5000, boot, d1), 6000, boot, d1)
        val b = StepCounter.update(a, 400, boot + 9_000_000, d1.plusDays(1))
        assertEquals(400, b.steps); assertEquals(0, b.baseline)
    }

    @Test fun longGapStartsFreshWithoutGuessing() {
        val a = StepCounter.update(StepCounter.update(null, 5000, boot, d1), 6000, boot, d1)
        val b = StepCounter.update(a, 20000, boot, d1.plusDays(3))
        assertEquals(0, b.steps); assertEquals(20000, b.baseline)
    }

    @Test fun negativeReadingIsClamped() {
        assertEquals(0, StepCounter.update(null, -5, boot, d1).lastReading.toInt())
    }
}
