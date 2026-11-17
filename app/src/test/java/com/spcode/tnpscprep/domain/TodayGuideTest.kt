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

    @Test fun planIsCappedAtThreeWithAtMostTwoRevisionsFirst() {
        val p = PlanBuilder.build(listOf(DuePick("a", false), DuePick("b", false), DuePick("c", true), DuePick("d", false)), "t1", "af1")
        assertEquals(listOf(PlanPick(PlanKind.REVISION, "a"), PlanPick(PlanKind.REVISION, "b"), PlanPick(PlanKind.READ, "t1")), p)
    }

    @Test fun planUsesMoreRevisionsWhenNothingIsNew() {
        val p = PlanBuilder.build(listOf(DuePick("a", false), DuePick("b", false), DuePick("c", true), DuePick("d", false)), null, null)
        assertEquals(listOf("a", "b", "c"), p.map { it.refId })
        assertEquals(PlanKind.NEWS, p[2].kind)
    }

    @Test fun planWithNoDataIsEmptyAndNeverRepeatsAnId() {
        assertTrue(PlanBuilder.build(emptyList(), null, null).isEmpty())
        assertEquals(1, PlanBuilder.build(listOf(DuePick("x", false)), "x", "x").size)
    }

    // ---- evening wrap-up: planned tasks in the rule order ----
    private val plan = PlannedTaskRef(7, "Read Preamble")
    private fun withPlan(exam: Boolean = true, due: Int = 0, focus: Int = 30, hour: Int = 10, food: Boolean = true, topic: Boolean = true, p: PlannedTaskRef? = plan) =
        NextActionInput(exam, due, focus, hour, food, topic, p)

    @Test fun plannedTaskBeatsNewTopic() = assertEquals(NextAction.PlannedTask(7, "Read Preamble"), TodayGuide.nextAction(withPlan()))
    @Test fun plannedTaskNeverBeatsDueRevision() = assertEquals(NextAction.Revise(1), TodayGuide.nextAction(withPlan(due = 1)))
    @Test fun plannedTaskNeverBeatsFocusOrFood() {
        assertEquals(NextAction.StartFocus, TodayGuide.nextAction(withPlan(focus = 0)))
        assertEquals(NextAction.LogFood, TodayGuide.nextAction(withPlan(hour = 20, food = false)))
    }
    @Test fun withoutPlanTheOldOrderIsUnchanged() {
        assertEquals(NextAction.NewTopic, TodayGuide.nextAction(withPlan(p = null)))
        assertEquals(NextAction.AllCaughtUp, TodayGuide.nextAction(withPlan(p = null, topic = false)))
    }
    @Test fun wrapupCardStates() {
        assertEquals(WrapupCardState.HIDDEN, WrapupCard.state(15, 18, false))
        assertEquals(WrapupCardState.INVITE, WrapupCard.state(18, 18, false))
        assertEquals(WrapupCardState.INVITE, WrapupCard.state(1, 18, false)) // 1 AM is still "tonight"
        assertEquals(WrapupCardState.HIDDEN, WrapupCard.state(3, 18, false)) // new day after 3 AM
        assertEquals(WrapupCardState.DONE, WrapupCard.state(21, 18, true))
        assertEquals(WrapupCardState.HIDDEN, WrapupCard.state(12, 18, true))
    }
}
