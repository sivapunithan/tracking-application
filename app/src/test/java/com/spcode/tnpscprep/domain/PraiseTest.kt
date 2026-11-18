package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    @Test fun everyEventHasAClipAndTheGoalChangesFocus() {
        PraiseEvent.entries.forEach { assertTrue(PraiseRules.soundFor(it).isNotBlank()) }
        assertEquals("proud", PraiseRules.soundFor(PraiseEvent.FOCUS_DONE)); assertEquals("celebrate", PraiseRules.soundFor(PraiseEvent.FOCUS_DONE, goalMet = true))
        assertEquals("boom", PraiseRules.soundFor(PraiseEvent.PLAN_COMPLETE))
        assertEquals("slap", MoodSoundRules.WRONG_PIN)
    }
    @Test fun topicAndWaterRules() {
        assertNull(MoodSoundRules.forTopicLevelUp(0)); assertEquals("cheer", MoodSoundRules.forTopicLevelUp(2)); assertEquals("celebrate", MoodSoundRules.forTopicLevelUp(4))
        assertEquals("celebrate", MoodSoundRules.forWater(1900, 250, 2000)); assertNull(MoodSoundRules.forWater(500, 250, 2000)); assertNull(MoodSoundRules.forWater(2100, 250, 2000))
        assertEquals("worried", MoodSoundRules.forFood("MIXED"))
    }
    @Test fun sounds() {
        assertEquals("cry", MoodSoundRules.forMood("CRY")); assertEquals("angry", MoodSoundRules.forMood("ANGRY")); assertEquals("worried", MoodSoundRules.forMood("WORRIED"))
        assertEquals("laugh", MoodSoundRules.forMood("LAUGH")); assertEquals("cheer", MoodSoundRules.forMood("CHEER")); assertEquals("proud", MoodSoundRules.forMood("PROUD")); assertEquals("celebrate", MoodSoundRules.forMood("CELEBRATE")); assertNull(MoodSoundRules.forMood(null))
        assertEquals("fail", MoodSoundRules.forFood("JUNK")); assertNull(MoodSoundRules.forFood("HEALTHY"))
    }
}
