package com.spcode.tnpscprep.domain

/** What the "Next best action" card suggests. Pure rules, no Android. */
sealed interface NextAction {
    data object SetExamDate : NextAction
    data class Revise(val dueCount: Int) : NextAction
    data object StartFocus : NextAction
    data object LogFood : NextAction
    data object NewTopic : NextAction
    data object AllCaughtUp : NextAction
}

data class NextActionInput(
    val hasExamDate: Boolean,
    val dueCount: Int,
    val focusMinutesToday: Int,
    val hour: Int,
    val foodLoggedToday: Boolean,
    val hasNewTopic: Boolean,
)

object TodayGuide {
    const val EVENING_HOUR = 18

    /**
     * Rule ORDER (first match wins):
     *  1. no exam date            -> set it (everything else is easier with a target)
     *  2. revisions are due       -> revise (the schedule is the app's core; overdue topics fade fastest)
     *  3. no focus session today  -> start one
     *  4. evening, food not logged-> log food (a 5-second habit, only worth asking late in the day)
     *  5. an unread topic exists  -> start a new topic
     *  6. otherwise               -> all caught up
     */
    fun nextAction(i: NextActionInput): NextAction = when {
        !i.hasExamDate -> NextAction.SetExamDate
        i.dueCount > 0 -> NextAction.Revise(i.dueCount)
        i.focusMinutesToday <= 0 -> NextAction.StartFocus
        i.hour >= EVENING_HOUR && !i.foodLoggedToday -> NextAction.LogFood
        i.hasNewTopic -> NextAction.NewTopic
        else -> NextAction.AllCaughtUp
    }
}

enum class GettingStartedStep { EXAM_DATE, FIRST_FOCUS, FIRST_FOOD, FIRST_DIARY }

data class GettingStartedItem(val step: GettingStartedStep, val done: Boolean)

object GettingStarted {
    fun items(examDateSet: Boolean, anySession: Boolean, anyFood: Boolean, anyDiary: Boolean): List<GettingStartedItem> = listOf(
        GettingStartedItem(GettingStartedStep.EXAM_DATE, examDateSet),
        GettingStartedItem(GettingStartedStep.FIRST_FOCUS, anySession),
        GettingStartedItem(GettingStartedStep.FIRST_FOOD, anyFood),
        GettingStartedItem(GettingStartedStep.FIRST_DIARY, anyDiary),
    )

    fun isComplete(items: List<GettingStartedItem>) = items.all { it.done }
}

/** One line of "Today's plan". Ids are topic ids / current-affairs ids; the UI resolves names. */
sealed interface PlanItem {
    data class Revise(val id: String, val isAffair: Boolean) : PlanItem
    data class NewTopic(val id: String) : PlanItem
    data class ReviewAffair(val id: String) : PlanItem
}

data class PlanSummary(val items: List<PlanItem>, val done: Int) {
    val total: Int get() = done + items.size
}

object TodayPlan {
    const val MAX_REVISIONS = 3

    /**
     * 3-5 items made of EXISTING data only: up to 3 due revisions, one new topic, one current-affairs item to review.
     * [doneToday] = revisions / first reads already done today (from the activity log); it counts as done work, so progress is "done of total"
     * and survives restarts without storing anything extra.
     */
    fun build(dueIds: List<Pair<String, Boolean>>, newTopicId: String?, affairId: String?, doneToday: Int): PlanSummary {
        val items = ArrayList<PlanItem>()
        dueIds.take(MAX_REVISIONS).forEach { (id, isAffair) -> items += PlanItem.Revise(id, isAffair) }
        if (newTopicId != null) items += PlanItem.NewTopic(newTopicId)
        if (affairId != null) items += PlanItem.ReviewAffair(affairId)
        return PlanSummary(items, doneToday.coerceAtLeast(0))
    }
}
