package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetRulesTest {
    @Test fun planLinesUnfinishedFirstMaxThreeTrimmed() {
        val l = WidgetRules.planLines(listOf("a" to true, "b" to false, "c" to true, "d" to false, "  e  " to false))
        assertEquals(listOf("b", "d", "e"), l.map { it.first }); assertEquals(listOf(false, false, false), l.map { it.second })
        assertEquals(70, WidgetRules.planLines(listOf("x".repeat(200) to false)).first().first.length)
    }
    @Test fun countdownHidesPastAndMissingDates() {
        assertNull(WidgetRules.countdown(null)); assertNull(WidgetRules.countdown(-1)); assertEquals(0L, WidgetRules.countdown(0)); assertEquals(30L, WidgetRules.countdown(30))
    }
    @Test fun minutesLineNeverNegativeOrZeroGoal() { assertEquals(0 to 1, WidgetRules.minutesLine(-5, 0)) }
}
