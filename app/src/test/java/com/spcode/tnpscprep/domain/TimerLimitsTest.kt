package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerLimitsTest {
    @Test fun step_moves_by_delta() = assertEquals(30, TimerLimits.step(25, 5, TimerLimits.FOCUS))
    @Test fun step_clamps_at_both_ends() {
        assertEquals(90, TimerLimits.step(90, 5, TimerLimits.FOCUS))
        assertEquals(5, TimerLimits.step(5, -5, TimerLimits.FOCUS))
        assertEquals(1, TimerLimits.step(1, -1, TimerLimits.BREAK))
        assertEquals(600, TimerLimits.step(595, 15, TimerLimits.GOAL))
    }
}
