package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RevisionLevelTest {
    @Test fun advance_walks_0_to_4() {
        var level = RevisionLevel.NOT_STARTED
        val seen = mutableListOf(level)
        repeat(4) { level = RevisionLevel.advance(level); seen += level }
        assertEquals(listOf(0, 1, 2, 3, 4), seen)
    }

    @Test fun advance_stops_at_max_and_does_not_wrap() {
        assertEquals(4, RevisionLevel.advance(4))
    }

    @Test fun out_of_range_values_are_clamped() {
        assertEquals(1, RevisionLevel.advance(-5))
        assertEquals(4, RevisionLevel.advance(99))
        assertEquals(0, RevisionLevel.clamp(-1))
    }

    @Test fun reset_returns_not_started() = assertEquals(0, RevisionLevel.reset())

    @Test fun percent_empty_is_zero() = assertEquals(0, RevisionLevel.percent(emptyList()))
    @Test fun percent_all_not_started_is_zero() = assertEquals(0, RevisionLevel.percent(listOf(0, 0, 0)))
    @Test fun percent_all_max_is_100() = assertEquals(100, RevisionLevel.percent(listOf(4, 4)))
    @Test fun percent_gives_partial_credit() {
        assertEquals(50, RevisionLevel.percent(listOf(2, 2)))
        assertEquals(25, RevisionLevel.percent(listOf(1)))
        assertEquals(33, RevisionLevel.percent(listOf(4, 0, 0))) // 4/12 = 33.3 -> rounds to 33
    }
}
