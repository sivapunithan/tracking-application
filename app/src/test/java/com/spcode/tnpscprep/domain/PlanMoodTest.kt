package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanMoodTest {
    @Test fun ladder() {
        assertEquals("CRY", PlanMood.of(0, 0, 10))
        assertEquals("PROUD", PlanMood.of(0, 4, 10))
        assertEquals("CHEER", PlanMood.of(1, 4, 10))
        assertEquals("LAUGH", PlanMood.of(2, 4, 10))
        assertEquals("CELEBRATE", PlanMood.of(4, 4, 10))
    }
    @Test fun worriedOnlyEveningWithNothingDone() {
        assertEquals("WORRIED", PlanMood.of(0, 3, 19))
        assertEquals("CHEER", PlanMood.of(1, 3, 19))
    }
    @Test fun titles() {
        assertNull(PlanMood.cleanTitle("   "))
        assertEquals(80, PlanMood.cleanTitle("x".repeat(200))!!.length)
    }
}
