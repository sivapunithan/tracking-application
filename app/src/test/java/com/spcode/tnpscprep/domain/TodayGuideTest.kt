package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayGuideTest {
    private fun input(
        exam: Boolean = true, due: Int = 0, focus: Int = 30, hour: Int = 10, food: Boolean = true, topic: Boolean = true,
    ) = NextActionInput(exam, due, focus, hour, food, topic)

    @Test fun missingExamDateComesFirstEvenWithDueRevisions() =
        assertEquals(NextAction.SetExamDate, TodayGuide.nextAction(input(exam = false, due = 5, focus = 0)))

    @Test fun dueRevisionsBeatTheFocusSession() =
        assertEquals(NextAction.Revise(2), TodayGuide.nextAction(input(due = 2, focus = 0)))

    @Test fun focusSessionWhenNothingIsDueAndNoneToday() =
        assertEquals(NextAction.StartFocus, TodayGuide.nextAction(input(focus = 0, hour = 20, food = false)))

    @Test fun foodOnlyInTheEveningAndOnlyIfNotLogged() {
        assertEquals(NextAction.LogFood, TodayGuide.nextAction(input(hour = 18, food = false)))
        assertEquals(NextAction.NewTopic, TodayGuide.nextAction(input(hour = 17, food = false)))
        assertEquals(NextAction.NewTopic, TodayGuide.nextAction(input(hour = 21, food = true)))
    }

    @Test fun newTopicThenAllCaughtUp() {
        assertEquals(NextAction.NewTopic, TodayGuide.nextAction(input()))
        assertEquals(NextAction.AllCaughtUp, TodayGuide.nextAction(input(topic = false)))
    }

    @Test fun gettingStartedCompletionNeedsAllFourSteps() {
        val partial = GettingStarted.items(examDateSet = true, anySession = true, anyFood = false, anyDiary = true)
        assertFalse(GettingStarted.isComplete(partial))
        assertEquals(listOf(true, true, false, true), partial.map { it.done })
        assertTrue(GettingStarted.isComplete(GettingStarted.items(true, true, true, true)))
        assertFalse(GettingStarted.isComplete(GettingStarted.items(false, false, false, false)))
    }

    @Test fun planHasAtMostThreeRevisionsPlusOneTopicPlusOneAffair() {
        val p = TodayPlan.build(listOf("a" to false, "b" to false, "c" to true, "d" to false), "t1", "af1", doneToday = 2)
        assertEquals(5, p.items.size)
        assertEquals(PlanItem.Revise("c", true), p.items[2])
        assertEquals(7, p.total); assertEquals(2, p.done)
    }

    @Test fun planWithNoDataIsEmptyAndDoneNeverNegative() {
        val p = TodayPlan.build(emptyList(), null, null, doneToday = -3)
        assertTrue(p.items.isEmpty()); assertEquals(0, p.done); assertEquals(0, p.total)
    }
}
