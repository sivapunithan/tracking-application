package com.spcode.tnpscprep.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeatmapTest {
    private fun focus(d: String, m: Int) = HeatEvent(d, "FOCUS_SESSION", "{\"minutes\":$m}")

    @Test fun levelsFollowGoal() {
        assertEquals(0, Heatmap.level(0, 0, 60))
        assertEquals(1, Heatmap.level(0, 2, 60))   // revised topics, no timer
        assertEquals(1, Heatmap.level(19, 1, 60))
        assertEquals(2, Heatmap.level(20, 1, 60))
        assertEquals(3, Heatmap.level(40, 1, 60))
        assertEquals(4, Heatmap.level(60, 1, 60))
    }

    @Test fun minutesOnlyFromFocusSessions() {
        assertEquals(25, Heatmap.minutesOf(focus("2026-11-17", 25)))
        assertEquals(0, Heatmap.minutesOf(HeatEvent("2026-11-17", "NOTE", "{\"minutes\":9}")))
        assertEquals(0, Heatmap.minutesOf(HeatEvent("2026-11-17", "FOCUS_SESSION", "{}")))
    }

    @Test fun summariseSumsPerDay() {
        val s = Heatmap.summarise(listOf(focus("2026-11-17", 25), focus("2026-11-17", 40), focus("bad", 5)), 60)
        assertEquals(1, s.size)
        assertEquals(65, s.getValue(LocalDate.of(2026, 11, 17)).minutes)
        assertEquals(4, s.getValue(LocalDate.of(2026, 11, 17)).level)
    }

    @Test fun gridEndsWithTodaysWeekAndHidesFuture() {
        val today = LocalDate.of(2026, 11, 17) // a Tuesday
        val g = Heatmap.grid(emptyMap(), today, 4)
        assertEquals(4, g.size)
        assertEquals(LocalDate.of(2026, 10, 26), g.first()[0]!!.date)
        assertEquals(today, g.last()[1]!!.date)
        assertNull(g.last()[2])
        assertEquals(Heatmap.firstDate(today, 4), g.first()[0]!!.date)
    }
}
