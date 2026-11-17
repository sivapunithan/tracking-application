package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecapBuilderTest {
    private val z = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 17)
    private fun ms(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(z).toInstant().toEpochMilli()
    private fun ev(type: ActivityType, d: LocalDate, h: Int, ref: String? = null, details: String = "{}", localDate: LocalDate = d) =
        RecapEvent(ms(d, h), localDate.toString(), type.name, ref, details)
    private val names = mapOf("t1" to "Fundamental Rights", "t2" to "Preamble")
    private fun build(events: List<RecapEvent>) = RecapBuilder.build(day, events, 60, names, z)

    @Test fun emptyDayIsEmptyRecap() { val r = build(emptyList()); assertTrue(r.isEmpty); assertEquals(0, r.studyMinutes); assertNull(r.food); assertNull(r.steps) }

    @Test fun partialDayShowsOnlyWhatHappened() {
        val r = build(listOf(ev(ActivityType.FOCUS_SESSION, day, 10, "s", "{\"minutes\":25}")))
        assertFalse(r.isEmpty)
        assertEquals(25, r.studyMinutes); assertEquals(1, r.focusSessions); assertEquals(25, r.longestSessionMinutes)
        assertTrue(r.topicsAdvanced.isEmpty()); assertNull(r.food); assertEquals(0, r.diaryEntries)
    }

    @Test fun fullDay() {
        val r = build(listOf(
            ev(ActivityType.FOCUS_SESSION, day, 9, "s", "{\"minutes\":25}"),
            ev(ActivityType.FOCUS_SESSION, day, 11, "s", "{\"minutes\":50}"),
            ev(ActivityType.TOPIC_PROGRESS, day, 12, "t1", "{\"from\":0,\"to\":1}"),
            ev(ActivityType.TOPIC_PROGRESS, day, 13, "t1", "{\"from\":1,\"to\":2}"), // same topic twice = once
            ev(ActivityType.TOPIC_PROGRESS, day, 14, "t2", "{\"from\":2,\"to\":3}"),
            ev(ActivityType.AFFAIR_REVISED, day, 15, "a1"),
            ev(ActivityType.FOOD_CHECKIN, day, 20, day.toString(), "{\"value\":\"HEALTHY\"}"),
            ev(ActivityType.STEPS, day, 21, day.toString(), "{\"steps\":4200}"),
            ev(ActivityType.DIARY, day, 21, "7"),
        ))
        assertEquals(75, r.studyMinutes); assertEquals(2, r.focusSessions); assertEquals(50, r.longestSessionMinutes)
        assertEquals(listOf("Fundamental Rights", "Preamble"), r.topicsAdvanced)
        assertEquals(1, r.affairsRevised); assertEquals(FoodState.HEALTHY, r.food); assertEquals(4200, r.steps); assertEquals(1, r.diaryEntries)
    }

    @Test fun resetsAndUnnamedTopicsAreNotListed() {
        val r = build(listOf(
            ev(ActivityType.TOPIC_PROGRESS, day, 10, "t1", "{\"from\":2,\"to\":0}"),
            ev(ActivityType.TOPIC_PROGRESS, day, 11, "unknown", "{\"from\":0,\"to\":1}"),
        ))
        assertTrue(r.topicsAdvanced.isEmpty())
    }

    @Test fun threeAmRolloverBelongsToThePreviousDay() {
        val next = day.plusDays(1)
        // 01:30 on the 18th still belongs to the 17th; 03:00 on the 18th belongs to the 18th
        val late = ev(ActivityType.FOCUS_SESSION, next, 1, "s", "{\"minutes\":30}")
        val morning = ev(ActivityType.FOCUS_SESSION, next, 3, "s", "{\"minutes\":40}")
        assertEquals(30, build(listOf(late, morning)).studyMinutes)
        assertEquals(40, RecapBuilder.build(next, listOf(late, morning), 60, names, z).studyMinutes)
    }

    @Test fun foodAndDiaryFollowTheirOwnLocalDate() {
        val next = day.plusDays(1)
        // written at 01:00 on the 18th but stamped with food day = the 17th (rollover applied at write time)
        val food = ev(ActivityType.FOOD_CHECKIN, next, 1, day.toString(), "{\"value\":\"JUNK\"}", localDate = day)
        val other = ev(ActivityType.FOOD_CHECKIN, next, 12, next.toString(), "{\"value\":\"HEALTHY\"}", localDate = next)
        assertEquals(FoodState.JUNK, build(listOf(food, other)).food)
    }

    @Test fun backfilledTopicRowsAreIgnored() {
        assertTrue(build(listOf(ev(ActivityType.TOPIC_PROGRESS, day, 10, "t1", "{\"from\":0,\"to\":1,\"backfilled\":true}"))).topicsAdvanced.isEmpty())
    }

    @Test fun topicCountIncludesUnnamedTopics() {
        val r = build(listOf(
            ev(ActivityType.TOPIC_PROGRESS, day, 10, "t1", "{\"from\":0,\"to\":1}"),
            ev(ActivityType.TOPIC_PROGRESS, day, 11, "unknown", "{\"from\":0,\"to\":1}"),
        ))
        assertEquals(2, r.topicsAdvancedCount); assertEquals(listOf("Fundamental Rights"), r.topicsAdvanced); assertFalse(r.isEmpty)
    }
}
