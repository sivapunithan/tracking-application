package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodEngineTest {
    private fun mood(streak: Int = 0, goalMet: Boolean = false, since: Int? = 0, overdue: Int = 0, hour: Int = 14) =
        MoodEngine.pick(MoodInput(streak, goalMet, since, overdue, hour))

    @Test fun a_brand_new_user_gets_an_encouraging_thumbs_up() = assertEquals(Mood.PROUD, mood(since = null))

    @Test fun long_absence_escalates_worried_then_crying_then_angry() {
        assertEquals(Mood.WORRIED, mood(since = 2))
        assertEquals(Mood.CRY, mood(since = 3))
        assertEquals(Mood.CRY, mood(since = 6))
        assertEquals(Mood.ANGRY, mood(since = 7))
        assertEquals(Mood.ANGRY, mood(since = 30))
    }

    @Test fun being_away_beats_an_old_streak_value() = assertEquals(Mood.CRY, mood(streak = 20, since = 4))

    @Test fun streak_milestones() {
        assertEquals(Mood.LAUGH, mood(streak = 7))
        assertEquals(Mood.LAUGH, mood(streak = 13))
        assertEquals(Mood.CELEBRATE, mood(streak = 14))
        assertEquals(Mood.CELEBRATE, mood(streak = 60))
    }

    @Test fun goal_met_cheers_on_a_short_streak_and_is_proud_otherwise() {
        assertEquals(Mood.CHEER, mood(streak = 3, goalMet = true))
        assertEquals(Mood.PROUD, mood(streak = 1, goalMet = true))
    }

    @Test fun many_overdue_topics_worry() = assertEquals(Mood.WORRIED, mood(overdue = 8))

    @Test fun evening_with_the_goal_not_reached_worries_but_a_met_goal_does_not() {
        assertEquals(Mood.WORRIED, mood(hour = 19, goalMet = false))
        assertEquals(Mood.PROUD, mood(hour = 21, goalMet = true))
        assertEquals(Mood.PROUD, mood(hour = 15, goalMet = false))
    }

    @Test fun the_set_is_exactly_the_seven_chosen_reactions_with_stable_image_names() {
        assertEquals(listOf("LAUGH", "CHEER", "PROUD", "WORRIED", "CRY", "ANGRY", "CELEBRATE"), Mood.entries.map { it.name })
        assertEquals("mood_cry", Mood.CRY.imageName)
    }
}
