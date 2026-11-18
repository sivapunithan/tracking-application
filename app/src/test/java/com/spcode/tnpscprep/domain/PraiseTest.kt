package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PraiseTest {
    @Test fun faces() {
        assertEquals(Mood.CELEBRATE, PraiseRules.moodFor(PraiseEvent.PLAN_COMPLETE))
        assertEquals(Mood.CELEBRATE, PraiseRules.moodFor(PraiseEvent.FOCUS_DONE, goalMet = true))
        assertEquals(Mood.PROUD, PraiseRules.moodFor(PraiseEvent.FOCUS_DONE))
    }
    @Test fun variantsCycleAndNeverNegative() {
        assertEquals(listOf(0, 1, 2, 0), (0..3).map { PraiseRules.variant(it) })
        assertEquals(2, PraiseRules.variant(-1))
    }
    @Test fun reminderFaceFollowsDaysAway() {
        assertEquals(Mood.CRY, PraiseRules.reminderMood(4, 20))
        assertEquals(Mood.ANGRY, PraiseRules.reminderMood(9, 20))
        assertEquals(Mood.PROUD, PraiseRules.reminderMood(null, 20))
    }
    @Test fun sounds() {
        assertEquals("cry", MoodSoundRules.forMood("CRY")); assertEquals("angry", MoodSoundRules.forMood("ANGRY")); assertEquals("worried", MoodSoundRules.forMood("WORRIED"))
        assertNull(MoodSoundRules.forMood("LAUGH")); assertNull(MoodSoundRules.forMood(null))
        assertEquals("fail", MoodSoundRules.forFood("JUNK")); assertNull(MoodSoundRules.forFood("HEALTHY"))
    }
}
